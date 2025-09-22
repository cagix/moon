(ns com.badlogic.gdx.application-listener
  (:require [com.badlogic.gdx :as gdx])
  (:import (com.badlogic.gdx ApplicationListener)))

(defn create
  [{:keys [create
           dispose
           render
           resize
           pause
           resume]
    :as listener}]
  (assert (and create
               dispose
               render
               resize
               pause
               resume)
          (str "Cant find all functions: (keys listener): " (pr-str (keys listener))))
  (reify ApplicationListener
    (create [_]
      (create (gdx/state)))
    (dispose [_]
      (dispose))
    (render [_]
      (render))
    (resize [_ width height]
      (resize width height))
    (pause [_]
      (pause))
    (resume [_]
      (resume))))
