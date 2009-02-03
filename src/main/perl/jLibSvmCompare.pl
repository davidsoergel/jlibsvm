#!/usr/bin/perl

$|++;

use strict;

use List::Util qw(sum);

sub main()
    {
    my @datasets = ("~/src/jlibsvm/src/test/resources/mushrooms", "~/src/jlibsvm/src/test/resources/sector");


    my @argsets = ( "-s 0 -t 0 -c 100 -e .01 -v 10 -m 1000",
                   "-s 0 -t 1 -c 100 -e .01 -v 10 -m 1000",
                   "-s 0 -t 2 -c 100 -e .01 -v 10 -m 1000",
                   "-s 0 -t 3 -c 100 -e .01 -v 10 -m 1000",
                   "-s 0 -t 4 -c 100 -e .01 -v 10 -m 1000",

                   "-s 1 -t 0 -c 100 -e .01 -v 10 -m 1000",
                   "-s 1 -t 1 -c 100 -e .01 -v 10 -m 1000",
                   "-s 1 -t 2 -c 100 -e .01 -v 10 -m 1000",
                   "-s 1 -t 3 -c 100 -e .01 -v 10 -m 1000",
                   "-s 1 -t 4 -c 100 -e .01 -v 10 -m 1000",

                   "-s 0 -t 0 -c 100 -e .001 -v 10 -m 1000",
                   "-s 0 -t 1 -c 100 -e .001 -v 10 -m 1000",
                   "-s 0 -t 2 -c 100 -e .001 -v 10 -m 1000",
                   "-s 0 -t 3 -c 100 -e .001 -v 10 -m 1000",
                   "-s 0 -t 4 -c 100 -e .001 -v 10 -m 1000",

                   "-s 1 -t 0 -c 100 -e .001 -v 10 -m 1000",
                   "-s 1 -t 1 -c 100 -e .001 -v 10 -m 1000",
                   "-s 1 -t 2 -c 100 -e .001 -v 10 -m 1000",
                   "-s 1 -t 3 -c 100 -e .001 -v 10 -m 1000",
                   "-s 1 -t 4 -c 100 -e .001 -v 10 -m 1000",
                  
                   "-s 0 -t 4 -c 10 -g .5 -e .01 -v 10 -m 1000",
                   "-s 1 -t 4 -c 10 -g .5 -e .01 -v 10 -m 1000"
                   );

    for my $dataset (@datasets)
    	{
       	for my $args (@argsets)
       	    {
            my %commandlines = ("LIBSVM-c" => "~/src-3rdparty/libsvm-2.88/svm-train $args $dataset",
                        "LIBSVM-j" => "java -cp ~/src-3rdparty/libsvm-2.88/java/libsvm.jar svm_train $args $dataset",
                        "jLibSvm" => "java -Xmx1500m -jar ~/src/jlibsvm/jlibsvm.jar $args $dataset");

            for my $commandname (keys %commandlines)
    	        {

               	my $command = $commandlines{$commandname};

               	print "-------- $command \n";

             	my @output = `/usr/bin/time -l $command 2>&1`;

              	my ($iterM, $iterSD, $nuM, $nuSD, $objM, $objSD, $rhoM, $rhoSD, $nsvM, $nsvSD, $nbsvM, $nbsvSD, $cvAcc, $cpu, $mem) = parse_output(@output);

                $mem = $mem / (1024*1024);

               	printf("%s, %s, %s, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.1f, %.2g, %.2f\n",
            	$commandname, $dataset, $args, $iterM, $iterSD, $nuM, $nuSD, $objM, $objSD, $rhoM, $rhoSD, $nsvM, $nsvSD, $nbsvM, $nbsvSD, $cvAcc, $cpu, $mem);
            	}
    	    }
    	}
    }

sub parse_output()
    {
    my(@output) = @_;

    # optimization finished, #iter = 620
	my @iter = grep (/./, map {/iter = (.*)/; $1} @output);

    # nu = 0.000021
	my @nu = grep (/./, map { /nu = (.*)/; $1} @output);

    # obj = -7.572607, rho = 0.137308
	my @obj = grep (/./, map { /obj = (.*?),/; $1} @output);
	my @rho = grep (/./, map { /rho = (.*)/; $1} @output);

    # nSV = 262, nBSV = 0
	my @nSV= grep (/./, map { /nSV = (.*?),/; $1} @output);
	my @nBSV = grep (/./, map { /nBSV = (.*)/; $1} @output);

    # Cross Validation Accuracy = 100%
	my @cvAcc = grep (/./, map { /Accuracy = (.*?)\%/; $1} @output);

	my @cpu = grep (/./, map { /.* (.*?) user/; $1} @output);
	my @mem = grep (/./, map { /\s*(.*)  maximum resident/; $1} @output);

    return (mean(@iter), stddev(@iter), mean(@nu), stddev(@nu), mean(@obj), stddev(@obj), mean(@rho), stddev(@rho), mean(@nSV), stddev(@nSV), mean(@nBSV), stddev(@nBSV), $cvAcc[0], $cpu[0], $mem[0]);
    }

sub mean
    {
    my @array = @_;
    if(@array == 0) { return 0; }
    return sum(@array)/@array;
    }

sub stddev
    {
    my @array = @_;
    if(@array == 0) { return 0; }
    my $m = mean(@array);
    my @devs = map {$_ - $m} @array;
    my @sqdevs = map {$_ * $_} @devs;
    return sqrt(sum(@sqdevs)/@sqdevs);
    }


main();
