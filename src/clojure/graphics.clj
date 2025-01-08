(ns clojure.graphics)

(defprotocol Graphics
  (delta-time [_] "The time span between the current frame and the last frame in seconds.")
  (frames-per-second [_] "The average number of frames per second."))
