(ns clojure.java.awt
  (:require [clojure.java.io :as io])
  (:require (java.awt Taskbar Toolkit)))

(defn set-taskbar-icon [resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource icon))))
