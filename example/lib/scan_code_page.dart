import 'package:flutter/cupertino.dart';

class ScanCodePage extends StatelessWidget {

  @override
  Widget build(BuildContext context) {
    var args = ModalRoute.of(context).settings.arguments;
    return Texture(textureId: args);
  }
}
