(ns anvil.app.create.stage
  (:require [anvil.app.create :as create]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn-impl create/stage []
  (stage/setup))
