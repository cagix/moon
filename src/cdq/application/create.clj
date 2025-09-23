(ns cdq.application.create
  (:require cdq.create.initial-record
            cdq.create.ctx-schema
            cdq.create.info
            cdq.create.txs
            cdq.create.load-entity-states
            cdq.create.load-effects
            cdq.create.input
            cdq.create.graphics
            cdq.create.stage
            cdq.create.set-input-processor
            cdq.create.audio
            cdq.create.reset-stage
            cdq.create.world
            cdq.create.reset-world
            cdq.create.spawn-player
            cdq.create.spawn-enemies
            cdq.impl.db
            cdq.ui.editor.overview-table
            cdq.ui.editor.window
            cdq.world-fns.tmx
            clojure.decl
            clojure.gdx.stage
            clojure.gdx.vis-ui
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [clojure.walk :as walk]))

(defn- require-resolve-symbols [form]
  (if (and (symbol? form)
           (namespace form))
    (let [avar (requiring-resolve form)]
      (assert avar form)
      avar)
    form))

(defn- edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk require-resolve-symbols)))

(def ^:private pipeline
  [
   [cdq.create.initial-record/merge-into-record]
   [cdq.create.ctx-schema/extend-validation
    [:map {:closed true}
     [:ctx/app :some]
     [:ctx/audio :some]
     [:ctx/db :some]
     [:ctx/graphics :some]
     [:ctx/world :some]
     [:ctx/input :some]
     [:ctx/controls :some]
     [:ctx/stage :some]
     [:ctx/vis-ui :some]
     [:ctx/mouseover-actor :any]
     [:ctx/ui-mouse-position :some]
     [:ctx/world-mouse-position :some]
     [:ctx/interaction-state :some]]]
   [cdq.ui.editor.overview-table/extend-ctx]
   [cdq.ui.editor.window/init!]
   [assoc
    :ctx/mouseover-actor nil
    :ctx/ui-mouse-position true
    :ctx/world-mouse-position true
    :ctx/interaction-state true]
   [clojure.decl/assoc* :ctx/db [cdq.impl.db/create {:schemas "schema.edn"
                                                     :properties "properties.edn"}]]
   [cdq.create.info/do!]
   [cdq.create.txs/do! (edn-resource "txs.edn")]
   [cdq.create.load-entity-states/do! (edn-resource "entity_states.edn")]
   [cdq.create.load-effects/do! (edn-resource "effects_fn_map.edn")]
   [assoc :ctx/controls {:zoom-in :minus
                         :zoom-out :equals
                         :unpause-once :p
                         :unpause-continously :space}]
   [cdq.create.input/do!]
   [clojure.decl/assoc* :ctx/vis-ui [clojure.gdx.vis-ui/load! {:skin-scale :x1}]]
   [cdq.create.graphics/do! (edn-resource "graphics.edn")]
   [cdq.create.stage/do! {:stage-impl clojure.gdx.stage/create}]
   [cdq.create.set-input-processor/do!]
   [cdq.create.audio/do! {:sound-names (edn-resource "sounds.edn")
                          :path-format "sounds/%s.wav"}]
   [dissoc :ctx/files]
   [cdq.create.reset-stage/do!]
   [cdq.create.world/do! (edn-resource "world.edn")]
   [cdq.create.reset-world/do! [cdq.world-fns.tmx/create {:tmx-file "maps/vampire.tmx"
                                                          :start-position [32 71]}]]
   [cdq.create.spawn-player/do!]
   [cdq.create.spawn-enemies/do!]
   ])

(defn do! [context]
  (utils/pipeline context pipeline))
