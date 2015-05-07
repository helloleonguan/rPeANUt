macro
putc &c
      load #&c R1
      store R1 0xfff0
mend

0x100 : putc 'h'
        putc 'e'
        putc 'l'
        putc 'l'
        putc 'o'
        putc '\n'





