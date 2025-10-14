(ns cdq.game.render.validate
  (:require [malli.core :as m]
            [malli.utils :as mu]))

; cdq depends on 'cdq.application' (libgdx stuff)
; cdq/audio , cdq.graphics, cdq.input, cdq.ui, cdq.world ?

(def ^:private schema
  (m/schema
   [:map {:closed true}

    ; TODO make separate libs !
    [:ctx/audio :some]

    [:ctx/db :some]

    [:ctx/graphics :some]

    [:ctx/input :some]

    [:ctx/stage :some]
    [:ctx/actor-fns :some]

    [:ctx/world :some]]))

(defn step [ctx]
  (mu/validate-humanize schema ctx)
  ctx)
