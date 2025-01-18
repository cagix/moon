(ns cdq.graphics.ui-viewport
  (:require [cdq.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create [config _context]
  {:pre [(:width  config)
         (:height config)]}
  (fit-viewport/create (:width  config)
                       (:height config)
                       (OrthographicCamera.)))
