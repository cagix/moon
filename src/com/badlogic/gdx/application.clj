(ns com.badlogic.gdx.application
  (:require gdl.application)
  (:import (com.badlogic.gdx Application)))

(extend-type Application
  gdl.application/Application
  (post-runnable! [this f]
    (.postRunnable this f)))
