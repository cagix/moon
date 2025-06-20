(ns gdl.create.graphics
  (:require [gdl.files :as files]
            [gdl.graphics]
            [gdl.graphics.g2d.batch :as batch]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.viewport :as viewport]
            [gdl.utils.assets :as assets]
            [gdl.utils.disposable]
            [gdx.graphics :as graphics]
            [gdx.graphics.color :as color]
            [gdx.graphics.g2d :as g2d]
            [gdx.graphics.g2d.freetype :as freetype]
            [gdx.graphics.shape-drawer :as sd]
            [gdx.tiled :as tiled]
            [gdx.utils.screen :as screen-utils])
  (:import (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defn- create-cursors [graphics files cursors cursor-path-format]
  (update-vals cursors
               (fn [[file hotspot]]
                 (graphics/create-cursor graphics
                                         (files/internal files (format cursor-path-format file))
                                         hotspot))))

(defrecord Graphics [
                     batch
                     cursors
                     default-font
                     graphics
                     shape-drawer-texture
                     shape-drawer
                     textures
                     tiled-map-renderer
                     ui-viewport
                     unit-scale
                     world-unit-scale
                     world-viewport
                     ]
  gdl.utils.disposable/Disposable
  (dispose! [_]
    (gdl.utils.disposable/dispose! batch)
    (gdl.utils.disposable/dispose! shape-drawer-texture)
    (run! gdl.utils.disposable/dispose! (vals textures))
    (run! gdl.utils.disposable/dispose! (vals cursors))
    (when default-font
      (gdl.utils.disposable/dispose! default-font)))

  gdl.graphics/Graphics
  (clear-screen! [_ color]
    (screen-utils/clear! color))

  (resize-viewports! [_ width height]
    (viewport/update! ui-viewport    width height true)
    (viewport/update! world-viewport width height false))

  (delta-time [_]
    (graphics/delta-time graphics))

  (frames-per-second [_]
    (graphics/frames-per-second graphics))

  (set-cursor! [_ cursor-key]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key)))

  ; TODO probably not needed I only work with texture-regions
  (texture [_ path]
    (assert (contains? textures path)
            (str "Cannot find texture with path: " (pr-str path)))
    (get textures path))

  (draw-on-world-viewport! [_ f]
    ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
    ; -> also Widgets, etc. ? check.
    (batch/set-color! batch (color/->obj :white))
    (batch/set-projection-matrix! batch (.combined (:camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (batch/end! batch))

  (draw-tiled-map! [_ tiled-map color-setter]
    (let [^OrthogonalTiledMapRenderer renderer (tiled-map-renderer tiled-map)
          camera (:camera world-viewport)]
      (.setColorSetter renderer (reify ColorSetter
                                  (apply [_ color x y]
                                    (color/float-bits (color-setter color x y)))))
      (.setView renderer camera)
      ; there is also:
      ; OrthogonalTiledMapRenderer/.renderTileLayer (TiledMapTileLayer layer)
      ; but right order / visible only ?
      (->> tiled-map
           tiled/layers
           (filter tiled/visible?)
           (map (partial tiled/layer-index tiled-map))
           int-array
           (.render renderer))))

  ; FIXME this can be memoized
  ; also good for tiled-map tiles they have to be memoized too
  (image->texture-region [graphics {:keys [image/file
                                           image/bounds]}]
    (assert file)
    (let [texture (gdl.graphics/texture graphics file)]
      (if bounds
        (apply texture/region texture bounds)
        (texture/region texture)))))

(comment
 (def texture-paths
   (vec (assets/search (.internal com.badlogic.gdx.Gdx/files "resources/")
                       #{"png" "bmp"})))
 )

(def texture-paths
   ["icon.png" "moon.png" "maps/uf_terrain.png" "maps/uf_items.png" "maps/floor.png" "maps/cliff.png" "maps/uf_heroes_simple.png" "images/rahmen.png" "images/items.png" "images/zoom.png" "images/fps.png" "images/uf_FX_impact.png" "images/mouseover.png" "images/moon_background.png" "images/mana.png" "images/hp.png" "images/clock.png" "images/uf_FX.png" "images/skills.png" "images/oryx_16bit_scifi_FX_lg_trans.png" "images/creatures.png" "images/uf_interface.png" "cursors/hand004_gray.png" "cursors/sandclock.png" "cursors/bag001.png" "cursors/x007.png" "cursors/pointer004.png" "cursors/hand004.png" "cursors/walking.png" "cursors/hand002.png" "cursors/hand003.png" "cursors/move002.png" "cursors/denied003.png" "cursors/default.png" "cursors/denied.png" "cursors/black_x.png" "images/animations/drow-4.png" "images/animations/slime-green-2.png" "images/animations/dragon-shadow-4.png" "images/animations/beetle-fire-4.png" "images/animations/rat-2.png" "images/animations/dwarf-mage-2.png" "images/animations/worm-giant-2.png" "images/animations/drow-bowman-3.png" "images/animations/lady-a-2.png" "images/animations/elemental-water-2.png" "images/animations/warden-1.png" "images/animations/lizardman-green-1.png" "images/animations/elemental-water-3.png" "images/animations/lady-a-3.png" "images/animations/drow-bowman-2.png" "images/animations/worm-giant-3.png" "images/animations/demon-red-1.png" "images/animations/dwarf-mage-3.png" "images/animations/barbarian-f-4.png" "images/animations/cultist-1.png" "images/animations/rat-3.png" "images/animations/bird-dove-1.png" "images/animations/slime-green-3.png" "images/animations/lord-b-4.png" "images/animations/lizardman-blue-spear-1.png" "images/animations/lizardman-blue-spear-3.png" "images/animations/slime-green-1.png" "images/animations/bird-dove-3.png" "images/animations/rat-1.png" "images/animations/cultist-3.png" "images/animations/demon-red-3.png" "images/animations/worm-giant-1.png" "images/animations/dwarf-mage-1.png" "images/animations/lich-4.png" "images/animations/lady-a-1.png" "images/animations/elemental-water-1.png" "images/animations/lizardman-green-3.png" "images/animations/warrior-f-4.png" "images/animations/warden-2.png" "images/animations/warden-3.png" "images/animations/lizardman-green-2.png" "images/animations/skeleton-4.png" "images/animations/drow-bowman-1.png" "images/animations/demon-red-2.png" "images/animations/cultist-2.png" "images/animations/bird-dove-2.png" "images/animations/lizardman-blue-spear-2.png" "images/animations/drow-2.png" "images/animations/slime-green-4.png" "images/animations/lord-b-3.png" "images/animations/beetle-fire-2.png" "images/animations/dragon-shadow-2.png" "images/animations/rat-4.png" "images/animations/worm-giant-4.png" "images/animations/dwarf-mage-4.png" "images/animations/barbarian-f-3.png" "images/animations/lady-a-4.png" "images/animations/lich-1.png" "images/animations/elemental-water-4.png" "images/animations/warrior-f-1.png" "images/animations/skeleton-1.png" "images/animations/drow-bowman-4.png" "images/animations/barbarian-f-2.png" "images/animations/dragon-shadow-3.png" "images/animations/beetle-fire-3.png" "images/animations/lord-b-2.png" "images/animations/drow-3.png" "images/animations/drow-1.png" "images/animations/beetle-fire-1.png" "images/animations/dragon-shadow-1.png" "images/animations/lich-2.png" "images/animations/skeleton-3.png" "images/animations/warden-4.png" "images/animations/warrior-f-2.png" "images/animations/warrior-f-3.png" "images/animations/lizardman-green-4.png" "images/animations/skeleton-2.png" "images/animations/lich-3.png" "images/animations/barbarian-f-1.png" "images/animations/demon-red-4.png" "images/animations/cultist-4.png" "images/animations/bird-dove-4.png" "images/animations/lord-b-1.png" "images/animations/lizardman-blue-spear-4.png" "images/animations/merchant-f-3.png" "images/animations/lady-b-3.png" "images/animations/minstrel-m-3.png" "images/animations/monk-3.png" "images/animations/templar-1.png" "images/animations/elemental-vorpal-1.png" "images/animations/mimic-1.png" "images/animations/elemental-earth-4.png" "images/animations/snake-giant-3.png" "images/animations/lord-a-4.png" "images/animations/vampire-lord-2.png" "images/animations/mummy-pharao-3.png" "images/animations/spider-brown-2.png" "images/animations/bird-crow-1.png" "images/animations/beetle-4.png" "images/animations/spider-brown-3.png" "images/animations/rat-giant-1.png" "images/animations/mummy-pharao-2.png" "images/animations/vampire-lord-3.png" "images/animations/halfling-wizard-4.png" "images/animations/drow-wizard-4.png" "images/animations/snake-giant-2.png" "images/animations/necromancer-4.png" "images/animations/ogre-1.png" "images/animations/toad-horned-1.png" "images/animations/spider-black-giant-1.png" "images/animations/elf-4.png" "images/animations/monk-2.png" "images/animations/wisp-ancient-4.png" "images/animations/moth-black-1.png" "images/animations/minstrel-m-2.png" "images/animations/golem-mud-4.png" "images/animations/lady-b-2.png" "images/animations/wraith-blue-4.png" "images/animations/merchant-f-2.png" "images/animations/moth-black-3.png" "images/animations/spider-black-giant-3.png" "images/animations/templar-2.png" "images/animations/skeleton-warrior-4.png" "images/animations/minotaur-4.png" "images/animations/mimic-2.png" "images/animations/elemental-vorpal-2.png" "images/animations/toad-horned-3.png" "images/animations/ogre-3.png" "images/animations/vampire-lord-1.png" "images/animations/spirit-greater-4.png" "images/animations/rat-giant-3.png" "images/animations/spider-brown-1.png" "images/animations/bird-crow-3.png" "images/animations/bird-crow-2.png" "images/animations/rat-giant-2.png" "images/animations/mummy-pharao-1.png" "images/animations/ogre-2.png" "images/animations/snake-giant-1.png" "images/animations/archer-4.png" "images/animations/toad-horned-2.png" "images/animations/mimic-3.png" "images/animations/elemental-vorpal-3.png" "images/animations/templar-3.png" "images/animations/spider-black-giant-2.png" "images/animations/wolf-black-4.png" "images/animations/monk-1.png" "images/animations/moth-black-2.png" "images/animations/minstrel-m-1.png" "images/animations/lady-b-1.png" "images/animations/skeleton-archer-4.png" "images/animations/merchant-f-1.png" "images/animations/wraith-blue-3.png" "images/animations/golem-mud-3.png" "images/animations/wisp-ancient-3.png" "images/animations/minotaur-1.png" "images/animations/elf-3.png" "images/animations/skeleton-warrior-1.png" "images/animations/elemental-earth-2.png" "images/animations/necromancer-3.png" "images/animations/vampire-lord-4.png" "images/animations/spirit-greater-1.png" "images/animations/drow-wizard-3.png" "images/animations/halfling-wizard-3.png" "images/animations/lord-a-2.png" "images/animations/spider-brown-4.png" "images/animations/beetle-3.png" "images/animations/beetle-2.png" "images/animations/mummy-pharao-4.png" "images/animations/lord-a-3.png" "images/animations/halfling-wizard-2.png" "images/animations/drow-wizard-2.png" "images/animations/archer-1.png" "images/animations/necromancer-2.png" "images/animations/elemental-earth-3.png" "images/animations/snake-giant-4.png" "images/animations/elf-2.png" "images/animations/minstrel-m-4.png" "images/animations/wisp-ancient-2.png" "images/animations/monk-4.png" "images/animations/wolf-black-1.png" "images/animations/golem-mud-2.png" "images/animations/lady-b-4.png" "images/animations/merchant-f-4.png" "images/animations/skeleton-archer-1.png" "images/animations/wraith-blue-2.png" "images/animations/skeleton-archer-3.png" "images/animations/wolf-black-3.png" "images/animations/mimic-4.png" "images/animations/elemental-vorpal-4.png" "images/animations/skeleton-warrior-2.png" "images/animations/minotaur-2.png" "images/animations/templar-4.png" "images/animations/elemental-earth-1.png" "images/animations/archer-3.png" "images/animations/spirit-greater-2.png" "images/animations/lord-a-1.png" "images/animations/beetle-1.png" "images/animations/bird-crow-4.png" "images/animations/rat-giant-4.png" "images/animations/halfling-wizard-1.png" "images/animations/drow-wizard-1.png" "images/animations/spirit-greater-3.png" "images/animations/toad-horned-4.png" "images/animations/archer-2.png" "images/animations/ogre-4.png" "images/animations/necromancer-1.png" "images/animations/spider-black-giant-4.png" "images/animations/skeleton-warrior-3.png" "images/animations/elf-1.png" "images/animations/minotaur-3.png" "images/animations/moth-black-4.png" "images/animations/wolf-black-2.png" "images/animations/wisp-ancient-1.png" "images/animations/golem-mud-1.png" "images/animations/wraith-blue-1.png" "images/animations/skeleton-archer-2.png" "images/animations/spider-black-1.png" "images/animations/zombie-a-3.png" "images/animations/pixie-a-4.png" "images/animations/prisoner-b-4.png" "images/animations/druid-4.png" "images/animations/dark-knight-1.png" "images/animations/valkyrie-a-1.png" "images/animations/golem-fire-4.png" "images/animations/wraith-red-2.png" "images/animations/bat-giant-4.png" "images/animations/goblin-warrior-3.png" "images/animations/lizardman-blue-2.png" "images/animations/dragon-red-1.png" "images/animations/helion-4.png" "images/animations/beetle-fire-giant-4.png" "images/animations/slime-red-1.png" "images/animations/barbarian-m-4.png" "images/animations/golem-metal-4.png" "images/animations/lizardman-blue-3.png" "images/animations/goblin-warrior-2.png" "images/animations/bird-hawk-1.png" "images/animations/wraith-red-3.png" "images/animations/wolf-brown-4.png" "images/animations/toad-blue-4.png" "images/animations/thief-4.png" "images/animations/toad-green-4.png" "images/animations/zombie-a-2.png" "images/animations/dragon-green-1.png" "images/animations/dragon-green-3.png" "images/animations/spider-black-2.png" "images/animations/dark-knight-2.png" "images/animations/valkyrie-a-2.png" "images/animations/elf-wizard-4.png" "images/animations/wraith-black-4.png" "images/animations/wraith-red-1.png" "images/animations/bird-hawk-3.png" "images/animations/dragon-red-2.png" "images/animations/lizardman-blue-1.png" "images/animations/lizardman-green-spear-4.png" "images/animations/slime-red-2.png" "images/animations/slime-red-3.png" "images/animations/slime-purple-4.png" "images/animations/dragon-red-3.png" "images/animations/bird-hawk-2.png" "images/animations/goblin-warrior-1.png" "images/animations/halfling-fighter-4.png" "images/animations/warrior-m-4.png" "images/animations/valkyrie-a-3.png" "images/animations/knight-4.png" "images/animations/dark-knight-3.png" "images/animations/spider-black-3.png" "images/animations/dragon-green-2.png" "images/animations/zombie-a-1.png" "images/animations/prisoner-b-2.png" "images/animations/druid-2.png" "images/animations/pixie-a-2.png" "images/animations/toad-green-3.png" "images/animations/elf-wizard-1.png" "images/animations/thief-3.png" "images/animations/wraith-black-1.png" "images/animations/golem-fire-2.png" "images/animations/toad-blue-3.png" "images/animations/wraith-red-4.png" "images/animations/bat-giant-2.png" "images/animations/wolf-brown-3.png" "images/animations/lizardman-green-spear-1.png" "images/animations/lizardman-blue-4.png" "images/animations/barbarian-m-2.png" "images/animations/beetle-fire-giant-2.png" "images/animations/golem-metal-3.png" "images/animations/helion-2.png" "images/animations/helion-3.png" "images/animations/golem-metal-2.png" "images/animations/beetle-fire-giant-3.png" "images/animations/barbarian-m-3.png" "images/animations/slime-purple-1.png" "images/animations/wolf-brown-2.png" "images/animations/bat-giant-3.png" "images/animations/goblin-warrior-4.png" "images/animations/toad-blue-2.png" "images/animations/golem-fire-3.png" "images/animations/halfling-fighter-1.png" "images/animations/warrior-m-1.png" "images/animations/thief-2.png" "images/animations/toad-green-2.png" "images/animations/pixie-a-3.png" "images/animations/druid-3.png" "images/animations/prisoner-b-3.png" "images/animations/knight-1.png" "images/animations/zombie-a-4.png" "images/animations/spider-black-4.png" "images/animations/prisoner-b-1.png" "images/animations/druid-1.png" "images/animations/knight-3.png" "images/animations/dark-knight-4.png" "images/animations/pixie-a-1.png" "images/animations/elf-wizard-2.png" "images/animations/valkyrie-a-4.png" "images/animations/wraith-black-2.png" "images/animations/halfling-fighter-3.png" "images/animations/warrior-m-3.png" "images/animations/golem-fire-1.png" "images/animations/bat-giant-1.png" "images/animations/lizardman-green-spear-2.png" "images/animations/dragon-red-4.png" "images/animations/slime-purple-3.png" "images/animations/beetle-fire-giant-1.png" "images/animations/barbarian-m-1.png" "images/animations/slime-red-4.png" "images/animations/helion-1.png" "images/animations/golem-metal-1.png" "images/animations/slime-purple-2.png" "images/animations/lizardman-green-spear-3.png" "images/animations/wolf-brown-1.png" "images/animations/bird-hawk-4.png" "images/animations/toad-blue-1.png" "images/animations/warrior-m-2.png" "images/animations/halfling-fighter-2.png" "images/animations/thief-1.png" "images/animations/wraith-black-3.png" "images/animations/elf-wizard-3.png" "images/animations/toad-green-1.png" "images/animations/knight-2.png" "images/animations/dragon-green-4.png" "images/animations/beholder-deep-4.png" "images/animations/dwarf-cleric-1.png" "images/animations/goblin-4.png" "images/animations/demon-blue-1.png" "images/animations/halfling-thief-1.png" "images/animations/golem-ice-3.png" "images/animations/minstrel-f-2.png" "images/animations/worm-4.png" "images/animations/wizard-2.png" "images/animations/elf-bowman-1.png" "images/animations/troll-2.png" "images/animations/lizardman-blue-shaman-2.png" "images/animations/goblin-shaman-3.png" "images/animations/golem-stone-1.png" "images/animations/mummy-4.png" "images/animations/moth-red-1.png" "images/animations/dragon-blue-1.png" "images/animations/merchant-m-2.png" "images/animations/elemental-fire-3.png" "images/animations/demon-green-3.png" "images/animations/beholder-1.png" "images/animations/zombie-b-2.png" "images/animations/spirit-2.png" "images/animations/spider-brown-giant-1.png" "images/animations/moth-white-4.png" "images/animations/beetle-giant-2.png" "images/animations/paladin-3.png" "images/animations/vampire-3.png" "images/animations/skeleton-mage-2.png" "images/animations/skeleton-mage-3.png" "images/animations/vampire-2.png" "images/animations/paladin-2.png" "images/animations/valkyrie-b-1.png" "images/animations/beetle-giant-3.png" "images/animations/prisoner-a-4.png" "images/animations/pixie-b-4.png" "images/animations/spirit-3.png" "images/animations/zombie-b-3.png" "images/animations/demon-green-2.png" "images/animations/merchant-m-3.png" "images/animations/elemental-fire-2.png" "images/animations/bone-dragon-1.png" "images/animations/goblin-shaman-2.png" "images/animations/lizardman-blue-shaman-3.png" "images/animations/troll-3.png" "images/animations/wizard-3.png" "images/animations/minstrel-f-3.png" "images/animations/golem-ice-2.png" "images/animations/lizardman-green-shaman-4.png" "images/animations/demon-blue-2.png" "images/animations/dwarf-cleric-2.png" "images/animations/halfling-thief-2.png" "images/animations/wisp-4.png" "images/animations/minstrel-f-1.png" "images/animations/troll-1.png" "images/animations/lizardman-blue-shaman-1.png" "images/animations/wizard-1.png" "images/animations/elf-bowman-2.png" "images/animations/bone-dragon-3.png" "images/animations/golem-stone-2.png" "images/animations/merchant-m-1.png" "images/animations/dragon-blue-2.png" "images/animations/moth-red-2.png" "images/animations/spider-brown-giant-2.png" "images/animations/zombie-b-1.png" "images/animations/spirit-1.png" "images/animations/beholder-2.png" "images/animations/beetle-giant-1.png" "images/animations/valkyrie-b-3.png" "images/animations/prisoner-c-4.png" "images/animations/skeleton-mage-1.png" "images/animations/dwarf-4.png" "images/animations/witch-4.png" "images/animations/vampire-1.png" "images/animations/valkyrie-b-2.png" "images/animations/paladin-1.png" "images/animations/flies-4.png" "images/animations/ogre-mystic-4.png" "images/animations/priest-4.png" "images/animations/beholder-3.png" "images/animations/spider-brown-giant-3.png" "images/animations/demon-green-1.png" "images/animations/dragon-blue-3.png" "images/animations/moth-red-3.png" "images/animations/elemental-fire-1.png" "images/animations/golem-stone-3.png" "images/animations/halfling-sling-4.png" "images/animations/bone-dragon-2.png" "images/animations/elf-bowman-3.png" "images/animations/goblin-shaman-1.png" "images/animations/snake-4.png" "images/animations/banshee-4.png" "images/animations/golem-ice-1.png" "images/animations/halfling-thief-3.png" "images/animations/elemental-air-4.png" "images/animations/dwarf-cleric-3.png" "images/animations/demon-blue-3.png" "images/animations/bat-4.png" "images/animations/lizardman-green-shaman-3.png" "images/animations/goblin-2.png" "images/animations/beholder-deep-2.png" "images/animations/worm-2.png" "images/animations/wisp-1.png" "images/animations/minstrel-f-4.png" "images/animations/troll-4.png" "images/animations/lizardman-blue-shaman-4.png" "images/animations/wizard-4.png" "images/animations/merchant-m-4.png" "images/animations/mummy-2.png" "images/animations/zombie-b-4.png" "images/animations/spirit-4.png" "images/animations/prisoner-a-3.png" "images/animations/moth-white-2.png" "images/animations/beetle-giant-4.png" "images/animations/pixie-b-3.png" "images/animations/prisoner-c-1.png" "images/animations/skeleton-mage-4.png" "images/animations/witch-1.png" "images/animations/dwarf-1.png" "images/animations/paladin-4.png" "images/animations/flies-1.png" "images/animations/vampire-4.png" "images/animations/ogre-mystic-1.png" "images/animations/pixie-b-2.png" "images/animations/priest-1.png" "images/animations/moth-white-3.png" "images/animations/prisoner-a-2.png" "images/animations/demon-green-4.png" "images/animations/halfling-sling-1.png" "images/animations/mummy-3.png" "images/animations/elemental-fire-4.png" "images/animations/snake-1.png" "images/animations/banshee-1.png" "images/animations/goblin-shaman-4.png" "images/animations/golem-ice-4.png" "images/animations/elemental-air-1.png" "images/animations/worm-3.png" "images/animations/beholder-deep-3.png" "images/animations/bat-1.png" "images/animations/goblin-3.png" "images/animations/lizardman-green-shaman-2.png" "images/animations/dwarf-cleric-4.png" "images/animations/demon-blue-4.png" "images/animations/goblin-1.png" "images/animations/bat-3.png" "images/animations/beholder-deep-1.png" "images/animations/worm-1.png" "images/animations/wisp-2.png" "images/animations/halfling-thief-4.png" "images/animations/elemental-air-3.png" "images/animations/elf-bowman-4.png" "images/animations/snake-3.png" "images/animations/banshee-3.png" "images/animations/dragon-blue-4.png" "images/animations/mummy-1.png" "images/animations/moth-red-4.png" "images/animations/golem-stone-4.png" "images/animations/halfling-sling-3.png" "images/animations/beholder-4.png" "images/animations/spider-brown-giant-4.png" "images/animations/moth-white-1.png" "images/animations/ogre-mystic-3.png" "images/animations/priest-3.png" "images/animations/flies-3.png" "images/animations/dwarf-3.png" "images/animations/prisoner-c-2.png" "images/animations/witch-3.png" "images/animations/witch-2.png" "images/animations/prisoner-c-3.png" "images/animations/dwarf-2.png" "images/animations/flies-2.png" "images/animations/valkyrie-b-4.png" "images/animations/pixie-b-1.png" "images/animations/priest-2.png" "images/animations/ogre-mystic-2.png" "images/animations/prisoner-a-1.png" "images/animations/halfling-sling-2.png" "images/animations/bone-dragon-4.png" "images/animations/banshee-2.png" "images/animations/snake-2.png" "images/animations/elemental-air-2.png" "images/animations/wisp-3.png" "images/animations/bat-2.png" "images/animations/lizardman-green-shaman-1.png" "sounds/oryx_8-bit_sounds/8-bit_sounds_cover.png" "maps/Winlu Fantasy Exterior/tilesets/Fantasy_Outside_A5.png" "maps/Winlu Fantasy Exterior/tilesets/Fantasy_Outside_A2.png" "maps/Winlu Fantasy Exterior/tilesets/Fantasy_Outside_D.png"])

(defn do!
  [{:keys [ctx/files
           ctx/graphics]}
   {:keys [textures
           cursors ; optional
           cursor-path-format ; optional
           default-font ; optional, could use gdx included (BitmapFont.)
           tile-size
           ui-viewport
           world-viewport]}]
  (let [batch (g2d/sprite-batch)

        shape-drawer-texture (graphics/white-pixel-texture)

        world-unit-scale (float (/ tile-size))

        ui-viewport (graphics/fit-viewport (:width  ui-viewport)
                                           (:height ui-viewport)
                                           (graphics/orthographic-camera))

        {:keys [folder extensions]} textures
        textures-to-load texture-paths #_(assets/search (files/internal files folder) extensions)
        ;(println "load-textures (count textures): " (count textures))
        textures (into {} (for [file textures-to-load]
                            [file (graphics/load-texture file)]))

        cursors (create-cursors graphics files cursors cursor-path-format)]
    (map->Graphics {:graphics graphics
                    :textures textures
                    :cursors cursors
                    :default-font (when default-font
                                    (freetype/generate-font (files/internal files (:file default-font))
                                                            (:params default-font)))
                    :world-unit-scale world-unit-scale
                    :ui-viewport ui-viewport
                    :world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                          world-height (* (:height world-viewport) world-unit-scale)]
                                      (graphics/fit-viewport world-width
                                                             world-height
                                                             (graphics/orthographic-camera :y-down? false
                                                                                           :world-width world-width
                                                                                           :world-height world-height)))
                    :batch batch
                    :unit-scale (atom 1)
                    :shape-drawer-texture shape-drawer-texture
                    :shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))
                    :tiled-map-renderer (memoize (fn [tiled-map]
                                                   (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                                (float world-unit-scale)
                                                                                batch)))})))
