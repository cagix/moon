(ns clojure.awt
  (:require clojure.java.io)
  (:import (java.awt Taskbar
                     Toolkit)))

(defn set-taskbar-icon! [path]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (clojure.java.io/resource path))))
