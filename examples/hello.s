; A simple "Hello" program. 


0x0100 :  load #'H' R1
          store R1 0xFFF0
          load #'e' R1
          store R1 0xFFF0
          load #'l' R1
          store R1 0xFFF0
          load #'l' R1
          store R1 0xFFF0
          load #'o' R1
          store R1 0xFFF0
          load #'\n' R1
          store R1 0xFFF0
          halt 
