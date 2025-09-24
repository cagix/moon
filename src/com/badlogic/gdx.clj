(ns com.badlogic.gdx
  (:require com.badlogic.gdx.application
            com.badlogic.gdx.audio
            com.badlogic.gdx.files
            com.badlogic.gdx.graphics
            com.badlogic.gdx.input
            com.badlogic.gdx.utils.disposable)
  (:import (com.badlogic.gdx Gdx)))

(defn get-state []
  {:ctx/app      Gdx/app
   :ctx/audio    Gdx/audio
   :ctx/files    Gdx/files
   :ctx/graphics Gdx/graphics
   :ctx/input    Gdx/input})
