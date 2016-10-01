module vcs_test;
  reg clock = 1;
  reg [1023:0] vcdfile = 0;
  reg [1023:0] vpdfile = 0;

  always #`CLOCK_PERIOD clock = ~clock;
  
  initial begin
    /*** VCD & VPD dump ***/
    if ($value$plusargs("vcdfile=%s", vcdfile)) begin
      $dumpfile(vcdfile);
      $dumpvars(0, test_harness);
    end
    if ($value$plusargs("waveform=%s", vpdfile)) begin
      $vcdplusfile(vpdfile);
      $vcdpluson(0);
      $vcdplusautoflushon;
    end
    if ($test$plusargs("vpdmem")) begin
      $vcdplusmemon;
    end
  end

  test_harness test_harness(clock);
endmodule
