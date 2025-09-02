(ns cdq.start.set-icon
  (:require [clojure.java.io :as io])
  (:import (java.awt Taskbar
                     Toolkit)))

(defn do! [path]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource path))))
