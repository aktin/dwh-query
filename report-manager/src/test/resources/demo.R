library(lattice)
x <- 1:10
graph <- barchart(runif(10) ~ x, horizontal=FALSE)
trellis.device('svg',file='barchart1.svg',width=8, height=4)
print(graph)
d <- dev.off()
