(ns clojure.gdx.app-listener
  (:import (com.badlogic.gdx ApplicationListener)))

(defn create-adapter [{:keys [create dispose render resize pause resume]}]
  (proxy [ApplicationListener] []
    (create  []              (when-let [[f params] create] (f params)))
    (dispose []              (when dispose (dispose)))
    (render  []              (when-let [[f params] render] (f params)))
    (resize  [width height]  (when resize  (resize width height)))
    (pause   []              (when pause   (pause)))
    (resume  []              (when resume  (resume)))))
