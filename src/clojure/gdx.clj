(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn state []
  {
   ::app      Gdx/app
   ::audio    Gdx/audio
   ::files    Gdx/files
   ::graphics Gdx/graphics
   ::input    Gdx/input
   }
  )
