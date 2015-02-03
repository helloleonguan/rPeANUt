0x0100 : 
start : load 0xFFF1 R0
         jumpz R0 start
         load 0xFFF0 R0 
next:         load 0xFFF1 R0
         jumpz R0 next
         load 0xFFF0 R0