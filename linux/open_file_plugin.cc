#include "include/open_file/open_file_plugin.h"

#include <flutter_linux/flutter_linux.h>
#include <gtk/gtk.h>
#include <sys/utsname.h>


#define OPEN_FILE_PLUGIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj), open_file_plugin_get_type(), \
                              OpenFilePlugin))

struct _OpenFilePlugin {
  GObject parent_instance;
    FlPluginRegistrar* registrar;

  // Connection to Flutter engine.
  FlMethodChannel* channel;
};

G_DEFINE_TYPE(OpenFilePlugin, open_file_plugin, G_TYPE_OBJECT)



// Converts a file type received from Flutter into a GTK file filter.
static GtkFileFilter* file_type_to_filter(FlValue* name, FlValue* extensions) {

  if(name == nullptr || fl_value_get_type(name) != FL_VALUE_TYPE_STRING ||
  extensions == nullptr || fl_value_get_type(extensions) != FL_VALUE_TYPE_STRING)
    return nullptr;

  g_autoptr(GtkFileFilter) filter = gtk_file_filter_new();
  gtk_file_filter_set_name(filter, g_strdup_printf("%s.%s", 
  fl_value_get_string(name), fl_value_get_string(extensions)));

  g_autofree gchar* pattern = g_strdup_printf("*.%s", fl_value_get_string(extensions));
  gtk_file_filter_add_pattern(filter, pattern);
  
  if (extensions == nullptr && fl_value_get_type(extensions) == FL_VALUE_TYPE_STRING)
    gtk_file_filter_add_pattern(filter, "*");

  return GTK_FILE_FILTER(g_object_ref(filter));
}




//------------------ start file -------------------
// Shows the requested dialog type.
static FlMethodResponse* show_dialog(OpenFilePlugin* self,
                                     GtkFileChooserAction action,
                                     const gchar* title,
                                     const gchar* default_confirm_button_text,
                                     FlValue* properties) {
  if (fl_value_get_type(properties) != FL_VALUE_TYPE_MAP) {
    return FL_METHOD_RESPONSE(fl_method_error_response_new(
        "Bad Arguments", "Argument map missing or malformed", nullptr));
  }


  FlView* view = fl_plugin_registrar_get_view(self->registrar);
  if (view == nullptr) {
    return FL_METHOD_RESPONSE(
        fl_method_error_response_new("No Screen", nullptr, nullptr));
  }

  GtkWindow* window = GTK_WINDOW(gtk_widget_get_toplevel(GTK_WIDGET(view)));
  g_autoptr(GtkFileChooserNative) dialog =
      GTK_FILE_CHOOSER_NATIVE(gtk_file_chooser_native_new(
          title, window, action, default_confirm_button_text, "_Cancel"));

  // value = fl_value_lookup_string(properties, kAllowsMultipleSelectionKey);
  // if (value != nullptr && fl_value_get_type(value) == FL_VALUE_TYPE_BOOL) {
  //   gtk_file_chooser_set_select_multiple(GTK_FILE_CHOOSER(dialog),
  //                                        fl_value_get_bool(value));
  // }


  // value = fl_value_lookup_string(properties, kCanChooseDirectoriesKey);
  // if (value != nullptr && fl_value_get_type(value) == FL_VALUE_TYPE_BOOL &&
  //     fl_value_get_bool(value)) {
  //   gtk_file_chooser_set_action(GTK_FILE_CHOOSER(dialog),
  //                               GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER);
  // }

  // value = fl_value_lookup_string(properties, kInitialDirectoryKey);
  // if (value != nullptr && fl_value_get_type(value) == FL_VALUE_TYPE_STRING) {
  //   gtk_file_chooser_set_current_folder(GTK_FILE_CHOOSER(dialog),
  //                                       fl_value_get_string(value));
  // }


  FlValue* value = fl_value_lookup_string(properties, "nameFile");
  if (value != nullptr && fl_value_get_type(value) == FL_VALUE_TYPE_STRING) {
    gtk_file_chooser_set_current_name(GTK_FILE_CHOOSER(dialog),
                                      fl_value_get_string(value));
  }

  FlValue* extension = fl_value_lookup_string(properties, "extension");
  value = fl_value_lookup_string(properties, "type");

  if (extension != nullptr && fl_value_get_type(extension) == FL_VALUE_TYPE_STRING &&
  value != nullptr && fl_value_get_type(value) == FL_VALUE_TYPE_STRING) {
    g_autoptr(GtkFileFilter) filter = file_type_to_filter(value, extension);
    if (filter == nullptr) {
      return FL_METHOD_RESPONSE(fl_method_error_response_new(
          "Bad Arguments", "Allowed file types malformed", nullptr));
    }
    gtk_file_chooser_add_filter(GTK_FILE_CHOOSER(dialog), filter);
    
  }



  gint response = gtk_native_dialog_run(GTK_NATIVE_DIALOG(dialog));
  g_autoptr(FlValue) result = nullptr;
  if (response == GTK_RESPONSE_ACCEPT) {
  
    result = fl_value_new_map();

    g_autofree gchar* filepath = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(dialog));
    fl_value_set_string_take(result, "path", fl_value_new_string(filepath));
  }

  return FL_METHOD_RESPONSE(fl_method_success_response_new(result));
}


// Called when a method call is received from Flutter.
static void open_file_plugin_handle_method_call(
    OpenFilePlugin* self,
    FlMethodCall* method_call) {
  const gchar* method = fl_method_call_get_name(method_call);
  FlValue* args = fl_method_call_get_args(method_call);

  g_autoptr(FlMethodResponse) response = nullptr;

  if (strcmp(method, "openFile") == 0) {
  response = show_dialog(self, GTK_FILE_CHOOSER_ACTION_OPEN, "Open File",
                            "_Open", args);

  } else if (strcmp(method, "createFile") == 0) {
    response = show_dialog(self, GTK_FILE_CHOOSER_ACTION_SAVE, "Save File",
                           "_Save", args);

  } else {
    response = FL_METHOD_RESPONSE(fl_method_not_implemented_response_new());
  }
  g_autoptr(GError) error = nullptr;
  if (!fl_method_call_respond(method_call, response, &error))
    g_warning("Failed to send method call response: %s", error->message);
}




static void open_file_plugin_dispose(GObject* object) {
  OpenFilePlugin* self = OPEN_FILE_PLUGIN(object);
  g_clear_object(&self->registrar);
  g_clear_object(&self->channel);
  G_OBJECT_CLASS(open_file_plugin_parent_class)->dispose(object);
}

static void open_file_plugin_class_init(OpenFilePluginClass* klass) {
  G_OBJECT_CLASS(klass)->dispose = open_file_plugin_dispose;
}




static void open_file_plugin_init(OpenFilePlugin* self) {}

static void method_call_cb(FlMethodChannel* channel, FlMethodCall* method_call,
                           gpointer user_data) {
  OpenFilePlugin* plugin = OPEN_FILE_PLUGIN(user_data);
  open_file_plugin_handle_method_call(plugin, method_call);
}



void open_file_plugin_register_with_registrar(FlPluginRegistrar* registrar) {
  OpenFilePlugin* plugin = OPEN_FILE_PLUGIN(
      g_object_new(open_file_plugin_get_type(), nullptr));

  plugin->registrar = FL_PLUGIN_REGISTRAR(g_object_ref(registrar));

  g_autoptr(FlStandardMethodCodec) codec = fl_standard_method_codec_new();
  plugin->channel =
      fl_method_channel_new(fl_plugin_registrar_get_messenger(registrar),
                            "open_file", FL_METHOD_CODEC(codec));
  fl_method_channel_set_method_call_handler(plugin->channel, method_call_cb,
                                            g_object_ref(plugin),
                                            g_object_unref);

  g_object_unref(plugin);
}
