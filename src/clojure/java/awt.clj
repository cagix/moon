(ns clojure.java.awt
  (:require [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)))

(defn set-taskbar-icon [io-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource io-resource))))
