#!/usr/bin/perl

$|++;

use strict;

use List::Util qw(sum);
use FileHandle;
use IPC::Open3;


#my $time = "/usr/bin/time -v"; # Ubuntu
#my $time = "/usr/bin/time -l"; # Mac

sub main()
	{
	my @datasets = ("~/src/jlibsvm/src/test/resources/mushrooms", "~/src/jlibsvm/src/test/resources/segment");  #"~/src/jlibsvm/src/test/resources/sector" too complex...

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
				print STDERR "$command \n";

				my $pid;
				#if (not defined($pid=open(COMMANDOUTPUT, "-|")))
				#my($wtr, $COMMANDOUTPUT,$err);
				my $COMMANDOUTPUT;
				#if (not defined($pid=open3($wtr, $COMMANDOUTPUT, $err, "-|")))
				if (not defined($pid=open(COMMANDOUTPUT, "-|")))
					{
					die "Can't fork: $!\n";
					}
				elsif ($pid == 0)
					{	# child process
					close STDERR;

                    #open STDERR, '>', "/tmp/jLibSvmCompare.stderr" or die "Can't redirect STDERR: $!";

					exec($command); # 2>&1 # "$time $command 2>&1"); #
					#for my $line (<STDERR>) { print $line; }
					# exits when done
					}

				# parent process
				#print("Watching PID: $pid\n");
				my $childAlive = 1;
				my $maxMem = 0;
				my $maxCpu = 0;
				while($childAlive)
					{
					my @top = `top -b -n 1 -S -p $pid`;
					#print("@top\n");
					my $top = @top[7];
					#print("7= $top\n");
					$top =~ s/^\s+//;

					@top = split /\s+/, $top;
					#print("split = " . join(",",@top) . "\n");

					if(@top == 0 || $top =~ /defunct/)
						{
						$maxCpu = @top[10];
						$childAlive = 0;
						}
					else
						{
						my $mem = @top[5];

						if($mem =~ s/k//)
							{
							$mem = $mem * 1024;
							}
						if($mem =~ s/m//)
							{
							$mem = $mem * 1024 * 1024;
							}
						if($mem =~ s/g//)
							{
							$mem = $mem * 1024 * 1024 * 1024;
							}

						if($mem > $maxMem)
							{
							$maxMem = $mem;
							}

						$maxCpu = @top[10];
						#print("$maxCpu $maxMem\n");
						sleep(5);
						}
					}

				#waitpid($pid,0);
				my @output = <COMMANDOUTPUT>;  # hope all the output got buffered even though the process is now dead
				close COMMANDOUTPUT;

				#print STDERR @output . " lines of output:" . "\n\n@output\n\n";

			#	open COMMANDERR,  "/tmp/jLibSvmCompare.stderr";
			#	my @err = <COMMANDERR>;
			#	close COMMANDERR;

			#	push(@output, @err);

				# wait for the command to complete and return status (like system does)
				# waitpid($pid, 0);
				# $status = $?;

				# my @output = `/usr/bin/time -l $command 2>&1`;


#, $cpu, $mem
				my ($iterM, $iterSD, $nuM, $nuSD, $objM, $objSD, $rhoM, $rhoSD, $nsvM, $nsvSD, $nbsvM, $nbsvSD, $cvAcc) = parse_output(@output);

				#$mem = $mem / (1024*1024);
				$maxMem = $maxMem / (1024*1024);

				$maxCpu =~ /(.*?):(.*)/;
				$maxCpu = $1 * 60 + $2;

				printf("%s, %s, %s, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.1f, %.2f, %.2f\n",
				$commandname, $dataset, $args, $iterM, $iterSD, $nuM, $nuSD, $objM, $objSD, $rhoM, $rhoSD, $nsvM, $nsvSD, $nbsvM, $nbsvSD, $cvAcc, $maxCpu, $maxMem);
				}
			}
		}
	}


sub parse_output()
	{
	my(@output) = @_;

	# this form didn't work reliably...??
	#my @iter = grep (/./, map {/iter = (.*)/; $1} @output);


	my @iter = ();
	my @nu = ();
	my @obj = ();
	my @rho = ();
	my @nSV = ();
	my @nBSV = ();
	my $cvAcc;

	foreach (@output)
		{
		# optimization finished, #iter = 620
		if(/iter = (.*)/)
			{
			push @iter, $1;
			}

		# nu = 0.000021
		if(/nu = (.*)/)
			{
			push @nu, $1;
			}

		# obj = -7.572607, rho = 0.137308
		if(/obj = (.*?),/)
			{
			push @obj, $1;
			}
		if(/rho = (.*)/)
			{
			push @rho, $1;
			}

		# nSV = 262, nBSV = 0
		if(/nSV = (.*?),/)
			{
			push @nSV, $1;
			}
		if(/nBSV = (.*)/)
			{
			push @nBSV, $1;
			}

		# Cross Validation Accuracy = 100%
		if(/Accuracy = (.*?)\%/)
			{
			$cvAcc = $1;
			}
		}

#	my @cpu = grep (/./, map { /.* (.*?) user/; $1} @output);
#	my @mem = grep (/./, map { /\s*(.*)  maximum resident/; $1} @output);

	return (mean(@iter), stddev(@iter), mean(@nu), stddev(@nu), mean(@obj), stddev(@obj), mean(@rho), stddev(@rho), mean(@nSV), stddev(@nSV), mean(@nBSV), stddev(@nBSV), $cvAcc); #, $cpu[0], $mem[0]);
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
