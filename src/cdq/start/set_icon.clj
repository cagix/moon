(ns cdq.start.set-icon
  (:require cdq.java.awt
            [clojure.java.io :as io]))

(defn do! [path]
  (cdq.java.awt/set-taskbar-icon! (io/resource path)))
