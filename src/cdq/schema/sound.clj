(ns cdq.schema.sound
  (:require [cdq.create.db :refer [malli-form]]))

(defmethod malli-form :s/sound [_ _schemas] :string)
