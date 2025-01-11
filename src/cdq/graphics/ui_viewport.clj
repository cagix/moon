(ns cdq.graphics.ui-viewport
  (:require [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create [context config]
  {:pre [(::width  config)
         (::height config)]}
  (assoc context :gdl.graphics/ui-viewport
         (fit-viewport/create (::width  config)
                              (::height config)
                              (OrthographicCamera.)
                              :center-camera? true)))
