(ns gdl.create.gdx
  (:require [gdl.gdx])
  (:import (com.badlogic.gdx Gdx)))

(defrecord Context [app
                    audio
                    files
                    graphics
                    input]
  gdl.gdx/Application
  (post-runnable! [_ runnable]
    (.postRunnable app runnable))
  )

(defn do! [_ctx _params]
  (map->Context {:app      Gdx/app
                 :audio    Gdx/audio
                 :files    Gdx/files
                 :graphics Gdx/graphics
                 :input    Gdx/input}))
