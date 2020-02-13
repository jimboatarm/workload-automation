#!/bin/bash
TARGET_OUT_DIR=$2
if [ $1 = "400.perlbench" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/400.perlbench/run/run_base_ref_none.0000 && time ./perlbench_base.none -I./lib checkspam.pl 2500 5 25 11 150 1 1 1 1  > '$TARGET_OUT_DIR'/perlbench.ref.checkspam.out'$3' 2>  '$TARGET_OUT_DIR'/perlbench.ref.checkspam.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/400.perlbench/run/run_base_ref_none.0000 && time ./perlbench_base.none -I./lib diffmail.pl 4 800 10 17 19 300  > '$TARGET_OUT_DIR'/perlbench.ref.diffmail.out'$3' 2>  '$TARGET_OUT_DIR'/perlbench.ref.diffmail.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/400.perlbench/run/run_base_ref_none.0000 && time ./perlbench_base.none -I./lib splitmail.pl 1600 12 26 16 4500  > '$TARGET_OUT_DIR'/perlbench.ref.splitmail.out'$3' 2>  '$TARGET_OUT_DIR'/perlbench.ref.splitmail.err'$3''
elif [ $1 = "401.bzip2" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/401.bzip2/run/run_base_ref_none.0000 && time ./bzip2_base.none chicken.jpg 30  > '$TARGET_OUT_DIR'/bzip2.ref.chicken.out'$3' 2>  '$TARGET_OUT_DIR'/bzip2.ref.chicken.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/401.bzip2/run/run_base_ref_none.0000 && time ./bzip2_base.none input.source 280  > '$TARGET_OUT_DIR'/bzip2.ref.source.out'$3' 2>  '$TARGET_OUT_DIR'/bzip2.ref.source.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/401.bzip2/run/run_base_ref_none.0000 && time ./bzip2_base.none liberty.jpg 30  > '$TARGET_OUT_DIR'/bzip2.ref.liberty.out'$3' 2>  '$TARGET_OUT_DIR'/bzip2.ref.liberty.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/401.bzip2/run/run_base_ref_none.0000 && time ./bzip2_base.none input.program 280  > '$TARGET_OUT_DIR'/bzip2.ref.program.out'$3' 2>  '$TARGET_OUT_DIR'/bzip2.ref.program.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/401.bzip2/run/run_base_ref_none.0000 && time ./bzip2_base.none text.html 280  > '$TARGET_OUT_DIR'/bzip2.ref.text.out'$3' 2>  '$TARGET_OUT_DIR'/bzip2.ref.text.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/401.bzip2/run/run_base_ref_none.0000 && time ./bzip2_base.none input.combined 200  > '$TARGET_OUT_DIR'/bzip2.ref.combined.out'$3' 2>  '$TARGET_OUT_DIR'/bzip2.ref.combined.err'$3''
elif [ $1 = "403.gcc" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none 166.in -o 166.s  > '$TARGET_OUT_DIR'/gcc.ref.166.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.166.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none 200.in -o 200.s  > '$TARGET_OUT_DIR'/gcc.ref.200.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.200.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none c-typeck.in -o c-typeck.s  > '$TARGET_OUT_DIR'/gcc.ref.c-typeck.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.c-typeck.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none cp-decl.in -o cp-decl.s  > '$TARGET_OUT_DIR'/gcc.ref.cp-decl.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.cp-decl.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none expr.in -o expr.s  > '$TARGET_OUT_DIR'/gcc.ref.expr.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.expr.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none expr2.in -o expr2.s  > '$TARGET_OUT_DIR'/gcc.ref.expr2.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.expr2.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none g23.in -o g23.s  > '$TARGET_OUT_DIR'/gcc.ref.g23.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.g23.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none s04.in -o s04.s  > '$TARGET_OUT_DIR'/gcc.ref.s04.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.s04.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/403.gcc/run/run_base_ref_none.0000 && time ./gcc_base.none scilab.in -o scilab.s  > '$TARGET_OUT_DIR'/gcc.ref.scilab.out'$3' 2>  '$TARGET_OUT_DIR'/gcc.ref.scilab.err'$3''
elif [ $1 = "429.mcf" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/429.mcf/run/run_base_ref_none.0000 && time ./mcf_base.none inp.in  > '$TARGET_OUT_DIR'/mcf.ref.out'$3' 2>  '$TARGET_OUT_DIR'/mcf.ref.err'$3''
elif [ $1 = "445.gobmk" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/445.gobmk/run/run_base_ref_none.0000 && time ./gobmk_base.none --quiet --mode gtp < 13x13.tst  > '$TARGET_OUT_DIR'/gobmk.ref.13x13.out'$3' 2>  '$TARGET_OUT_DIR'/gobmk.ref.13x13.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/445.gobmk/run/run_base_ref_none.0000 && time ./gobmk_base.none --quiet --mode gtp < nngs.tst  > '$TARGET_OUT_DIR'/gobmk.reff.nngs.out'$3' 2>  '$TARGET_OUT_DIR'/gobmk.ref.nngs.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/445.gobmk/run/run_base_ref_none.0000 && time ./gobmk_base.none --quiet --mode gtp < score2.tst  > '$TARGET_OUT_DIR'/gobmk.ref.score2.out'$3' 2>  '$TARGET_OUT_DIR'/gobmk.ref.score2.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/445.gobmk/run/run_base_ref_none.0000 && time ./gobmk_base.none --quiet --mode gtp < trevorc.tst  > '$TARGET_OUT_DIR'/gobmk.ref.trevorc.out'$3' 2>  '$TARGET_OUT_DIR'/gobmk.ref.trevorc.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/445.gobmk/run/run_base_ref_none.0000 && time ./gobmk_base.none --quiet --mode gtp < trevord.tst  > '$TARGET_OUT_DIR'/gobmk.ref.trevord.out'$3' 2>  '$TARGET_OUT_DIR'/gobmk.ref.trevord.err'$3''
elif [ $1 = "456.hmmer" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/456.hmmer/run/run_base_ref_none.0000 && time ./hmmer_base.none nph3.hmm swiss41  > '$TARGET_OUT_DIR'/hmmer.ref.nph3.out'$3' 2>  '$TARGET_OUT_DIR'/hmmer.ref.nph3.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/456.hmmer/run/run_base_ref_none.0000 && time ./hmmer_base.none --fixed 0 --mean 500 --num 500000 --sd 350 --seed 0 retro.hmm  > '$TARGET_OUT_DIR'/hmmer.ref.retro.out'$3' 2>  '$TARGET_OUT_DIR'/hmmer.ref.retro.err'$3''
elif [ $1 = "458.sjeng" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/458.sjeng/run/run_base_ref_none.0000 && time ./sjeng_base.none ref.txt  > '$TARGET_OUT_DIR'sjeng.ref.out'$3' 2>  '$TARGET_OUT_DIR'/sjeng.ref.err'$3''
elif [ $1 = "462.libquantum" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/462.libquantum/run/run_base_ref_none.0000 && time ./libquantum_base.none 1397 8  > '$TARGET_OUT_DIR'/libquantum.ref.out'$3' 2>  '$TARGET_OUT_DIR'libquantum.ref.err'$3''
elif [ $1 = "464.h264ref" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/464.h264ref/run/run_base_ref_none.0000 && time ./h264ref_base.none -d foreman_ref_encoder_baseline.cfg  > '$TARGET_OUT_DIR'/h264ref.ref.foreman_baseline.out'$3' 2>  '$TARGET_OUT_DIR'/h264ref.ref.foreman_baseline.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/464.h264ref/run/run_base_ref_none.0000 && time ./h264ref_base.none -d foreman_ref_encoder_main.cfg  > '$TARGET_OUT_DIR'/h264ref.ref.foreman_main.out'$3' 2>  '$TARGET_OUT_DIR'/h264ref.ref.foreman_main.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/464.h264ref/run/run_base_ref_none.0000 && time ./h264ref_base.none -d sss_encoder_main.cfg  > '$TARGET_OUT_DIR'/h264ref.ref.sss.out'$3' 2>  '$TARGET_OUT_DIR'/h264ref.ref.sss.err'$3''
elif [ $1 = "471.omnetpp" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/471.omnetpp/run/run_base_ref_none.0000 && time ./omnetpp_base.none omnetpp.ini  > '$TARGET_OUT_DIR'/omnetpp.ref.out'$3' 2>  '$TARGET_OUT_DIR'/omnetpp.ref.err'$3''
elif [ $1 = "473.astar" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/473.astar/run/run_base_ref_none.0000 && time ./astar_base.none BigLakes2048.cfg  > '$TARGET_OUT_DIR'/astar.ref.BigLakes2048.out'$3' 2>  '$TARGET_OUT_DIR'/astar.ref.BigLakes2048.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/473.astar/run/run_base_ref_none.0000 && time ./astar_base.none rivers.cfg  > '$TARGET_OUT_DIR'/astar.ref.rivers.out'$3' 2>  '$TARGET_OUT_DIR'/astar.ref.rivers.err'$3''
elif [ $1 = "483.xalancbmk" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/483.xalancbmk/run/run_base_ref_none.0000 && time ./Xalan_base.none -v t5.xml xalanc.xsl  > '$TARGET_OUT_DIR'/xalancbmk.ref.out'$3' 2>  '$TARGET_OUT_DIR'/xalancbmk.ref.err'$3'' 
elif [ $1 = "410.bwaves" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/410.bwaves/run/run_base_ref_none.0000 && time ./bwaves_base.none  > '$TARGET_OUT_DIR'/bwaves.ref.out'$3' 2> '$TARGET_OUT_DIR'/bwaves.ref.err'$3''
elif [ $1 = "416.gamess" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/416.gamess/run/run_base_ref_none.0000 && time ./gamess_base.none < cytosine.2.config  > '$TARGET_OUT_DIR'/gamess.ref.cytosine.out'$3' 2>  '$TARGET_OUT_DIR'/mess.ref.cytosine.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/416.gamess/run/run_base_ref_none.0000 && time ./gamess_base.none < h2ocu2+.gradient.config  > '$TARGET_OUT_DIR'/gamess.ref.h2ocu2+.out'$3' 2>  '$TARGET_OUT_DIR'/gamess.ref.h2ocu2+.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/416.gamess/run/run_base_ref_none.0000 && time ./gamess_base.none < triazolium.config  > '$TARGET_OUT_DIR'/gamess.ref.triazolium.out 2>  '$TARGET_OUT_DIR'/gamess.ref.triazolium.err'$3''
elif [ $1 = "433.milc" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/433.milc/run/run_base_ref_none.0000 && time ./milc_base.none < su3imp.in  > '$TARGET_OUT_DIR'/milc.ref.out'$3' 2>  '$TARGET_OUT_DIR'/milc.ref.err'$3''
elif [ $1 = "434.zeusmp" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/434.zeusmp/run/run_base_ref_none.0000 && time ./zeusmp_base.none  > '$TARGET_OUT_DIR'/zeusmp.ref.out'$3' 2>  '$TARGET_OUT_DIR'/zeusmp.ref.err'$3''
elif [ $1 = "435.gromacs" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/435.gromacs/run/run_base_ref_none.0000 && time ./gromacs_base.none -silent -deffnm gromacs -nice 0  > '$TARGET_OUT_DIR'/gromacs.ref.out'$3' 2>  '$TARGET_OUT_DIR'/gromacs.ref.err''$3'
elif [ $1 = "436.cactusADM" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/436.cactusADM/run/run_base_ref_none.0000 && time ./cactusADM_base.none benchADM.par  > '$TARGET_OUT_DIR'/cactusADM.ref.out'$3' 2>  '$TARGET_OUT_DIR'/cactusADM.ref.err'$3''
elif [ $1 = "437.leslie3d" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/437.leslie3d/run/run_base_ref_none.0000 && time ./leslie3d_base.none < leslie3d.in  > '$TARGET_OUT_DIR'/leslie3d.ref.out'$3' 2>  '$TARGET_OUT_DIR'/leslie3d.ref.err'$3''
elif [ $1 = "444.namd" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/444.namd/run/run_base_ref_none.0000 && time ./namd_base.none --input namd.input --iterations 38 --output namd.out  > '$TARGET_OUT_DIR'/namd.ref.out'$3' 2>  '$TARGET_OUT_DIR'/namd.ref.err'$3''
elif [ $1 = "447.dealII" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/447.dealII/run/run_base_ref_none.0000 && time ./dealII_base.none 23  > '$TARGET_OUT_DIR'/dealII.ref.out'$3' 2>  '$TARGET_OUT_DIR'/dealII.ref.err'$3''
elif [ $1 = "450.soplex" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/450.soplex/run/run_base_ref_none.0000 && time ./soplex_base.none -s1 -e -m45000 pds-50.mps  > '$TARGET_OUT_DIR'/soplex.ref.pds-50.out'$3' 2>  '$TARGET_OUT_DIR'/soplex.ref.pds-50.err'$3''
    sh -c 'cd /data/local/tmp/spec2k6/450.soplex/run/run_base_ref_none.0000 && time ./soplex_base.none -m3500 ref.mps  > '$TARGET_OUT_DIR'/soplex.ref.ref.out'$3' 2>  '$TARGET_OUT_DIR'/soplex.ref.ref.err'$3''
elif [ $1 = "453.povray" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/453.povray/run/run_base_ref_none.0000 && time ./povray_base.none SPEC-benchmark-ref.ini  > '$TARGET_OUT_DIR'/povray.ref.out'$3' 2>  '$TARGET_OUT_DIR'/povray.ref.err'$3''
elif [ $1 = "454.calculix" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/454.calculix/run/run_base_ref_none.0000 && time ./calculix_base.none -i hyperviscoplastic  > '$TARGET_OUT_DIR'/calculix.ref.out'$3' 2>  '$TARGET_OUT_DIR'/calculix.ref.err'$3''
elif [ $1 = "459.GemsFDTD" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/459.GemsFDTD/run/run_base_ref_none.0000 && time ./GemsFDTD_base.none  > '$TARGET_OUT_DIR'/GemsFDTD.ref.out'$3' 2>  '$TARGET_OUT_DIR'/GemsFDTD.ref.err'$3''
elif [ $1 = "465.tonto" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/465.tonto/run/run_base_ref_none.0000 && time ./tonto_base.none  > '$TARGET_OUT_DIR'/tonto.ref.out'$3' 2>  '$TARGET_OUT_DIR'/tonto.ref.err'$3''
elif [ $1 = "470.lbm" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/470.lbm/run/run_base_ref_none.0000 && time ./lbm_base.none 3000 reference.dat 0 0 100_100_130_ldc.of  > '$TARGET_OUT_DIR'/lbm.ref.out'$3' 2>  '$TARGET_OUT_DIR'/lbm.ref.err'$3''
elif [ $1 = "481.wrf" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/481.wrf/run/run_base_ref_none.0000 && time ./wrf_base.none  > '$TARGET_OUT_DIR'/wrf.ref.out'$3' 2>  '$TARGET_OUT_DIR'/wrf.ref.err'$3''
elif [ $1 = "482.sphinx3" ]
then
    sh -c 'cd /data/local/tmp/spec2k6/482.sphinx3/run/run_base_ref_none.0000 && time ./sphinx_livepretend_base.none ctlfile . args.an4  > '$TARGET_OUT_DIR'/sphinx3.ref.out'$3' 2>  '$TARGET_OUT_DIR'/sphinx3.ref.err'$3''
fi
