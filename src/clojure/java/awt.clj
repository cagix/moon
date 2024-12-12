(ns clojure.java.awt
  (:require [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)))

(defn set-taskbar-icon
  "On MacOs this is the dock-icon."
  [icon]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource icon))))
