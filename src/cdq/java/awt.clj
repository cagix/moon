(ns cdq.java.awt
  (:import (java.awt Taskbar
                     Toolkit)))

(defn set-taskbar-icon! [resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            resource)))
