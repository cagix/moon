(ns anvil.app.create.stage
  (:require [anvil.app.create :as create]
            [gdl.stage :as stage]))

(defn-impl create/stage []
  (stage/setup))
