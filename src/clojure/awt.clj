(ns clojure.awt
  (:require [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)))

(defn set-dock-icon [path]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource path))))
