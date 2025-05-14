(ns cdq.game.set-dock-icon
  (:require [clojure.java.io :as io])
  (:import (java.awt Taskbar
                     Toolkit)))

(defn do! [path]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource path))))
