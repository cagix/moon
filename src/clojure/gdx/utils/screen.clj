(ns clojure.gdx.utils.screen
  (:require [clojure.gdx.graphics.color :as color])
  (:import (com.badlogic.gdx.utils ScreenUtils)))

(defn clear! []
  (ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK))
