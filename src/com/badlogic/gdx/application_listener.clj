(ns com.badlogic.gdx.application-listener
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)))

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
          (str "Cant find all functions: (keys listener): "(pr-str (keys listener))))
  (reify ApplicationListener
    (create [_]
      (create {:gdl/app      Gdx/app
               :gdl/audio    Gdx/audio
               :gdl/files    Gdx/files
               :gdl/graphics Gdx/graphics
               :gdl/input    Gdx/input}))
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
