
import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

enum FileType {
  any,
  text,
  image,
  video,
  audio,
  custom
}


class FileInfo {
  String path;
  String nameFile;
  String size;
  File file;

  FileInfo({
    this.path,
    this.nameFile,
    this.size,
    this.file });
}



class OpenFile {
  static const MethodChannel _channel =
      const MethodChannel('open_file');



  static Future<FileInfo> openFile({
    FileType type = FileType.any,
    String extension = ""
  }) async {
    final Map fileMap = await _channel.invokeMethod('openFile',
      {
        "type": type.toString().split('.').last,
        "extension": extension,
      });
    File file = File(fileMap["path"]);

    return FileInfo(
      path : fileMap["path"],
      nameFile: fileMap["nameFile"] ?? fileMap["path"].split('/').last,
      size: fileMap["size"] ?? (await file.length()).toString(),
      file: File(fileMap["path"])
    );
  }



  static Future<FileInfo> createFile(FileType type, String extension, String name) async {
    final Map fileMap = await _channel.invokeMethod('createFile',
      {
        "type": type.toString().split('.').last,
        "extension": extension,
        "nameFile": name,
      });

    return FileInfo(
      path : fileMap["path"],
      nameFile: fileMap["nameFile"] ?? fileMap["path"].split('/').last,
      size: fileMap["size"] ?? 0,
      file: File(fileMap["path"])
    );
  }


}
