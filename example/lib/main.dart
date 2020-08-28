import 'dart:io';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:open_file/open_file.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _fileContent = 'Unknown';
  FileInfo fileInfo;
  TextEditingController _editingController = TextEditingController();

  @override
  void initState() {
    super.initState();
  }


  Future<void> _openFile() async {
    String content;
    try {
      fileInfo = (await OpenFile.openFile(type: FileType.text, extension: "cryp"));
      content = fileInfo.path+ "***"+ fileInfo.nameFile;
    } on PlatformException {
      content = 'Failed to rum File';
    }

    setState(() {
      _fileContent = content;
    });
  }


  Future<File> _write(){
    return fileInfo.file.writeAsString(_editingController.text);
  }



  Future<void> _createFile() async {
    String content;
    try {
      fileInfo = (await OpenFile.createFile(FileType.text, "cryp", "newName"));
      content = await fileInfo.file.readAsString();
    } on PlatformException {
      content = 'Failed to get platform version.';
    }

    setState(() {
      _fileContent = content;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Center(
                child: RaisedButton(
                  onPressed: ()=> _openFile(),
                  child: Text('open file'),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Text("valor do arquivo :$_fileContent"),
              ),
              Center(
                child: RaisedButton(
                  onPressed: _write,
                  child: Text('save file'),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: TextFormField(controller: _editingController),
              )
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: _createFile,
          tooltip: 'Increment',
          child: Icon(Icons.add),
        ),
      ),

    );
  }
}
