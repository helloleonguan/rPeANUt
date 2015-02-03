
0x100 : load #hellostr R1
loop :  load R1 R2
        jumpz R2 done
        push R2
        call putchar
        pop R2
        add R1 ONE R1
        jump loop
done : halt

hellostr : block #"Hello\n"

#include "io.s"
