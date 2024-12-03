(ns forge.roots.awt
  (:import (java.awt Taskbar Toolkit)))

(defn set-dock-icon [image-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            image-resource)))
