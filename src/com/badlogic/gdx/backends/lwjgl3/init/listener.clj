(ns com.badlogic.gdx.backends.lwjgl3.init.listener
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)))

(defn- application-listener
  [{:keys [create!
           dispose!
           render!
           resize!
           pause!
           resume!]}]
  (reify ApplicationListener
    (create [_]
      (create! {:clojure.gdx/app      Gdx/app
                :clojure.gdx/audio    Gdx/audio
                :clojure.gdx/files    Gdx/files
                :clojure.gdx/graphics Gdx/graphics
                :clojure.gdx/input    Gdx/input}))
    (dispose [_]
      (dispose!))
    (render [_]
      (render!))
    (resize [_ width height]
      (resize! width height))
    (pause [_]
      (pause!))
    (resume [_]
      (resume!))))

(defn do! [init]
  (update init :init/listener application-listener))
