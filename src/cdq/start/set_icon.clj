(ns cdq.start.set-icon
  (:require cdq.java.awt
            [clojure.java.io :as io]))

(defn do! []
  (cdq.java.awt/set-taskbar-icon! (io/resource "icon.png")))
