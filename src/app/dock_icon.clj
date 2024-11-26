(ns app.dock-icon
  (:require [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)))

(defn set-image [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))
