(ns cdq.app
  (:require [gdl.app :as app]
            [cdq.game :as game])
  (:gen-class))

(def state (atom nil))

(comment

 (clojure.pprint/pprint
  (sort (keys @state)))
 (:cdq.context/content-grid
  :cdq.context/entity-ids
  :cdq.context/error
  :cdq.context/explored-tile-corners
  :cdq.context/factions-iterations
  :cdq.context/grid
  :cdq.context/level
  :cdq.context/mouseover-eid
  :cdq.context/paused?
  :cdq.context/player-eid
  :cdq.context/player-message
  :cdq.context/raycaster
  :cdq.context/tiled-map
  :clojure.gdx/app
  :clojure.gdx/audio
  :clojure.gdx/files
  :clojure.gdx/gl
  :clojure.gdx/gl20
  :clojure.gdx/gl30
  :clojure.gdx/gl31
  :clojure.gdx/gl32
  :clojure.gdx/graphics
  :clojure.gdx/input
  :clojure.gdx/net
  :context/g
  :gdl.context/assets
  :gdl.context/db
  :gdl.context/elapsed-time
  :gdl.context/stage)

 (satisfies? gdl.utils/Disposable
             (:gdl.context/stage @state))

 )

(defn post-runnable [f]
  (app/post-runnable @state #(f @state)))

(defrecord Listener []
  app/Listener
  (create  [this config]       (game/create!  this config))
  (dispose [this]              (game/dispose! this))
  (render  [this]              (game/render!  this))
  (resize  [this width height] (game/resize!  this width height)))

(defn -main []
  (app/start state (Listener.)))
