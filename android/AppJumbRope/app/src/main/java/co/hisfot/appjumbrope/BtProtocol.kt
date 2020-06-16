package co.hisfot.appjumbrope

data class BtProtocol( var jumpCnt: String ) {
    var btProtocolByte : ByteArray = ByteArray(100);
    var index : Int = 0;

    fun addByte( byte: ByteArray ) : Boolean{
        println(byte);
        for( b in byte ){

            when (b.toInt()) {
                94 -> { // "^"
                    index = 0;
                    btProtocolByte = ByteArray(100)
                }
                59 -> { // ";"
                    var objCopy: ByteArray? = null
                    objCopy = ByteArray(index);
                    System.arraycopy(btProtocolByte, 0, objCopy, 0, index )
                    jumpCnt = objCopy.toString(Charsets.UTF_8);
                    index = 0;
                    return true;
                }
                else -> {
                    btProtocolByte[index] = b;
                    index++
                    if( index >= 100) index = 0;
                }
            }
        }
        return false;
    }


}