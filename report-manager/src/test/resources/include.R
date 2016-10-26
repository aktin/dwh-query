x <- 1:10
graph <- barchart(runif(10) ~ x, horizontal=FALSE, xlab="Unicode Text ÄÖÜäöü§€ line")
# svg
trellis.device('svg',file='barchart1.svg',width=8, height=4)
print(graph)
d <- dev.off()
report.generatedFile('barchart1.svg')

# png
trellis.device('png',file='barchart1.png')
print(graph)
d <- dev.off()
report.generatedFile('barchart1.png')
