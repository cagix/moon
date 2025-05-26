(ns cdq.start
  (:require [cdq.application :as application]
            [gdl.utils :as utils]))

(defn -main [config-path]
  (-> config-path
      utils/create-config
      application/start!))

(require 'gdl.application)

(require 'cdq.render)
(require 'cdq.render.render-entities)

; TODO this is the issue we know everywhere about the exact context ...
(extend gdl.application.Context
  cdq.render/Render
  {:render-entities! cdq.render.render-entities/render-entities!})

; TODO this doesn;t work when we reload gdl.application.Context -> we have to depend on it in ns form then the namespaces using it will be reloaded fine.
