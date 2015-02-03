
; void putchar(char c) - outputs the character c to the terminal
;    uses and clears R0
; stack: #0 - return address
;        #-1 - c

putchar : load SP #-1 R0
          store R0 0xFFF0
          return
