(ns app.dock-icon
  (:refer-clojure :exclude [set])
  (:import (java.awt Taskbar Toolkit)))

(defn set [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (clojure.java.io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))
