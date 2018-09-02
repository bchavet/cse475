#!/usr/bin/perl -w

my $failed = 0;

my @K = qw(1 5 10 500);
my @N = qw(0 10 50 100 500);
my @V = qw(1 15 50);

#Begin Outputting In CSV Format
print "Firefighter Count,Fires Started,Visibility,FF Health Average,FF Health Variance,FF Living Average,FF Living Variance,Fuel Start Average,Fuel Start Variance,Fuel Left Average,Fuel Left Variance\n";

foreach $k (@K)
{
	foreach $n (@N)
	{
		foreach $v (@V)
		{
#			print "Now opening N$n.K$k.V$v/results.txt\n";
			open(FH, "simulations/N$n.K$k.V$v/results.txt") or $failed = 1;
			if(!$failed)
			{
				handleResults(FH, $n, $k, $v);
				close(FH);
			}

			if($failed)
			{
				print STDERR "Failed to open N$n.K$k.V$v.\n";
				$failed = 0;
			}

		}
	}
}

sub handleResults
{
	my $fh = shift;
	my $n  = shift;
	my $k  = shift;
	my $v  = shift;

	my @favelife  = ();
	my @favealive = ();
	my @favefuelstart = ();
	my @favefuelend   = ();

	my @runfavelife  = ();

	$number  = "[0-9.]+";
#	$word    = "[a-zA-Z]+";
	$slash   = "/";

	while($line = <$fh>)
	{
		if($line =~ m/Run: $number/ || $line =~ m/Batch Done/)
		{
			push(@favelife, getAverage(@runfavelife));

			@runfavelife = ();
		}
		elsif($line =~ m/($number) $slash ($number) firefighters still alive/i)
		{
			push(@favealive, $1);
		}
		elsif($line =~ m/Firefighter $number: ($number)/i)
		{
			push(@runfavelife, $1);
		}
		elsif($line =~ m/($number) $slash ($number) total fuel remaining/i)
		{
			push(@favefuelend, $1);
			push(@favefuelstart, $2);
		}
	}

	$aveAlive = getAverage(@favealive);
	$aveLife  = getAverage(@favelife);
	$aveFuelStart = getAverage(@favefuelstart);
	$aveFuelEnd   = getAverage(@favefuelend);

	$varAlive = getVariance(@favealive, $aveAlive);
	$varLife  = getVariance(@favelife,  $aveLife);
	$varFuelStart = getVariance(@favefuelstart, $aveFuelStart);
	$varFuelEnd   = getVariance(@favefuelend,  $aveFuelEnd);

	print "$n,$k,$v,$aveLife,$varLife,$aveAlive,$varAlive,$aveFuelStart,$varFuelStart,$aveFuelEnd,$varFuelEnd\n";

}

sub printArray
{
	my @a = @_;
	my $line = "";

	foreach $var (@a)
	{
		$line .= $var . ", ";
	}

	return $line;
}

sub getAverage
{
	my @values = @_;

	my $total = 0;
	my $count = 0;

	foreach $val (@values)
	{
		$total += $val;
		$count++;
	}

	if($count == 0)
	{
		return 0;
	}
	return ($total/$count);
}

sub getVariance
{
	my @values = @_;
	my $average = shift;

	foreach $val (@values)
	{
		$res += ($val - $average)**2;
	}

	return sqrt($res);
}

sub getVariance2
{
	my @values  = @_;
	my $average = shift;

	my $count = 1;
	my $u     = 0;
	my $res   = 0;

	foreach $val (@values)
	{
		$u += $count * $val;
		$count++;
	}

	$count = 1;
	foreach $val (@values)
	{
		$res += ($count - $u)**2 * $val;
	}

	return $res;
}

