#!/usr/bin/perl

$|++;

use strict;

use List::Util qw(sum);


sub main()
	{
	chdir "~/src/jlibsvm/src/test/resources";  # doesn't work

	my @datasets = ("segment.scale", "news20", "letter.scale", "mushrooms", "sector.scale");
	#my @datasets = ("~/src/jlibsvm/src/test/resources/segment.scale");
	#my @datasets = ("~/src/jlibsvm/src/test/resources/letter.scale");
	#my @datasets = ("~/src/jlibsvm/src/test/resources/news20");
	#"~/src/jlibsvm/src/test/resources/sector" too complex...


    my @argsets = (
                    "-s 0 -t 2 -c 10 -g .5 -e .01 -m 1000",       # training only, no testing
                    "-s 0 -t 2 -c 10 -g .5 -e .01 -v 5 -m 1000",

                    "-s 0 -t 0 -c 100 -e .01 -v 5 -m 1000",
                   "-s 0 -t 1 -c 100 -e .01 -v 5 -m 1000",
                   "-s 0 -t 2 -c 100 -e .01 -v 5 -m 1000",
                   "-s 0 -t 3 -c 100 -e .01 -v 5 -m 1000",

            #       "-s 1 -t 0 -c 100 -e .01 -v 5 -m 1000",
            #       "-s 1 -t 1 -c 100 -e .01 -v 5 -m 1000",
            #       "-s 1 -t 2 -c 100 -e .01 -v 5 -m 1000",
            #       "-s 1 -t 3 -c 100 -e .01 -v 5 -m 1000",

                   "-s 0 -t 0 -c 100 -e .001 -v 5 -m 1000",
                   "-s 0 -t 1 -c 100 -e .001 -v 5 -m 1000",
                   "-s 0 -t 2 -c 100 -e .001 -v 5 -m 1000",
                   "-s 0 -t 3 -c 100 -e .001 -v 5 -m 1000",

            #       "-s 1 -t 0 -c 100 -e .001 -v 5 -m 1000",
            #       "-s 1 -t 1 -c 100 -e .001 -v 5 -m 1000",
            #       "-s 1 -t 2 -c 100 -e .001 -v 5 -m 1000",
            #       "-s 1 -t 3 -c 100 -e .001 -v 5 -m 1000",

            #        "-s 0 -t 2 -c 10 -g .5 -e .01 -v 5 -m 1000",
            #       "-s 1 -t 2 -c 10 -g .5 -e .01 -v 5 -m 1000"
                   );

	my %commandlines = ("LIBSVM-c" => "~/src-3rdparty/libsvm-2.88/svm-train $args $dataset",
			"LIBSVM-j" => "java -Xmx$mem -cp ~/src-3rdparty/libsvm-2.88/java/libsvm.jar svm_train $args $dataset",
			"jLibSvm None AllVsAll" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o None -a AllVsAll -j 0.1 $dataset"
			, "jLibSvm Best None" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -b 1 -o Best -k 0.2 -a None $dataset",
			"jLibSvm BreakTies AllVsAll" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o BreakTies -a AllVsAll  -j 0.1 $dataset",
			"jLibSvm Veto AllVsAll" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o Veto -k 0.2 -a AllVsAll  -j 0.1 $dataset",
			"jLibSvm Veto FilteredVsAll" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o Veto -k 0.2 -a FilteredVsAll  -j 0.1 $dataset",
			"jLibSvm Veto FilteredVsFiltered" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o Veto -k 0.2 -a FilteredVsFiltered  -j 0.1 $dataset",
            "jLibSvm VetoAndBreakTies AllVsAll" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o VetoAndBreakTies -k 0.2 -a AllVsAll -j 0.1 $dataset",
            "jLibSvm VetoAndBreakTies FilteredVsAll" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o VetoAndBreakTies -k 0.2 -a FilteredVsAll -j 0.1 $dataset",
			"jLibSvm VetoAndBreakTies FilteredVsFiltered" => "java -Xmx$mem -jar ~/src/jlibsvm/jlibsvm.jar $args -o VetoAndBreakTies -k 0.2 -a FilteredVsFiltered -j 0.1 $dataset"
            );

    print("<HTML><HEAD><meta http-equiv='refresh' content='10'></HEAD><BODY><TABLE border='1'>\n");
    print("<TR><TD>program</TD><TD>iter</TD><TD>nu</TD><TD>obj</TD><TD>rho</TD><TD>nSV</TD><TD>nBSV</TD><TD>cpu</TD><TD>mem</TD><TD>^unk</TD><TD>cvAcc</TD></TR>\n");

    my $mem = "4G"; # 1500m

	for my $dataset (@datasets)
		{
		for my $args (@argsets)
			{
			print "<TR><TD span=15><B>$args $dataset<B></TD></TR>\n";

		

			for my $commandname (keys %commandlines)
				{
				my $command = $commandlines{$commandname};
				print STDERR "$command \n";

				my $pid;
				#if (not defined($pid=open(COMMANDOUTPUT, "-|")))
				#my($wtr, $COMMANDOUTPUT,$err);
				my $COMMANDOUTPUT;
				#if (not defined($pid=open3($wtr, $COMMANDOUTPUT, $err, "-|")))
				if (not defined($pid=open($COMMANDOUTPUT, "-|")))
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

				my @output = ();

				# parent process
				#print("Watching PID: $pid\n");
				my $childAlive = 1;
				my $maxMemKb = 0;
				my $maxCpu = 0;
				while($childAlive)
					{
					my @ps = `ps S -o time,rss,state $pid`;

					my $ps = $ps[1];
					$ps =~ s/^\s+//;

					@ps = split /\s+/, $ps;

					# print STDERR join (",",@ps) . "\n";

					if(@ps == 0 || $ps[2] =~ /Z/)
						{
						$childAlive = 0;
						}

					my $memKb = $ps[1];
                    $memKb = $memKb;

                    if($memKb > $maxMemKb)
                        {
                        $maxMemKb = $memKb;
                        }

                    if($ps[0] =~ /[123456789]/)
                        {
                        $maxCpu = $ps[0];
                        #print STDERR "MAXCPU = $maxCpu\n";
                        }

                    sleep(1);

                    my ($eof,@lines) = nonblockGetLines($COMMANDOUTPUT, 1);
                    foreach (@lines)
                        {
                        push @output,$_;
						print STDERR "$_\n";
                        }

					#while(<COMMANDOUTPUT>)
					#	{
					#	push @output,$_;
					#	print STDERR;
					#	}
					}

				#waitpid($pid,0);
				#my @output = <COMMANDOUTPUT>;  # hope all the output got buffered even though the process is now dead

				while(<COMMANDOUTPUT>)
						{
						push @output,$_;
						print STDERR;
						}
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
				my ($iterM, $iterSD, $nuM, $nuSD, $objM, $objSD, $rhoM, $rhoSD, $nsvM, $nsvSD, $nbsvM, $nbsvSD, $cvClassified, $cvAcc) = parse_output(@output);

				#$mem = $mem / (1024*1024);
				my $maxMemMb = $maxMemKb / 1024;


				if($maxCpu =~ /(.*?):(.*?):(.*)/)
				    {
			    	$maxCpu = $1 * 60 * 60 + $2 * 60 + $3;
                    }
                elsif($maxCpu =~ /(.*?):(.*?)\.(.*)/)
				    {
			    	$maxCpu = $1 * 60 + $2 + $3 * .01;
                    }

				#printf("%s, %s, %s, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.2g, %.1f, %.2f, %.2f\n",
				#$commandname, $dataset, $args, $iterM, $iterSD, $nuM, $nuSD, $objM, $objSD, $rhoM, $rhoSD, $nsvM, $nsvSD, $nbsvM, $nbsvSD, $cvAcc, $maxCpu, $maxMemMb);

	            printf("<TR><TD>%s</TD>", $commandname);
	            printf("<TD>%.2f <FONT size='1'>+- %.2f</FONT></TD>", $iterM, $iterSD);
	            printf("<TD>%.2f <FONT size='1'>+- %.2f</FONT></TD>", $nuM, $nuSD);
	            printf("<TD>%.2f <FONT size='1'>+- %.2f</FONT></TD>", $objM, $objSD);
	            printf("<TD>%.2f <FONT size='1'>+- %.2f</FONT></TD>", $rhoM, $rhoSD);
	            printf("<TD>%.2f <FONT size='1'>+- %.2f</FONT></TD>", $nsvM, $nsvSD);
	            printf("<TD>%.2f <FONT size='1'>+- %.2f</FONT></TD>", $nbsvM, $nbsvSD);
	            printf("<TD>%.2f</TD><TD>%.2f</TD><TD>%.1f</TD><TD>%.1f</TD></TR>\n", $maxCpu, $maxMemMb, $cvClassified, $cvAcc);

				}
			}
		}

	print("</TABLE></BODY></HTML>");
	}

# http://davesource.com/Solutions/20080924.Perl-Non-blocking-Read-On-Pipes-Or-Files.html

# An non-blocking filehandle read that returns an array of lines read
# Returns:  ($eof,@lines)
my %nonblockGetLines_last;
sub nonblockGetLines {
	my ($fh,$timeout) = @_;

	$timeout = 0 unless defined $timeout;
	my $rfd = '';
	$nonblockGetLines_last{$fh} = ''
		unless defined $nonblockGetLines_last{$fh};

	vec($rfd,fileno($fh),1) = 1;
	return unless select($rfd, undef, undef, $timeout)>=0;
	# I'm not sure the following is necessary?
	return unless vec($rfd,fileno($fh),1);
	my $buf = '';
	my $n = sysread($fh,$buf,1024*1024);
	# If we're done, make sure to send the last unfinished line
	return (1,$nonblockGetLines_last{$fh}) unless $n;
	# Prepend the last unfinished line
	$buf = $nonblockGetLines_last{$fh}.$buf;
	# And save any newly unfinished lines
	$nonblockGetLines_last{$fh} =
		(substr($buf,-1) !~ /[\r\n]/ && $buf =~ s/([^\r\n]*)$//) ? $1 : '';
	$buf ? (0,split(/\n/,$buf)) : (0);
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
	my $cvClassified;

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

		# Cross Validation Classified = 100%
		if(/Classified = (.*?)\%/)
			{
			$cvClassified = $1;
			}
		}

#	my @cpu = grep (/./, map { /.* (.*?) user/; $1} @output);
#	my @mem = grep (/./, map { /\s*(.*)  maximum resident/; $1} @output);

	return (mean(@iter), stddev(@iter), mean(@nu), stddev(@nu), mean(@obj), stddev(@obj), mean(@rho), stddev(@rho), mean(@nSV), stddev(@nSV), mean(@nBSV), stddev(@nBSV), $cvClassified, $cvAcc); #, $cpu[0], $mem[0]);
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
