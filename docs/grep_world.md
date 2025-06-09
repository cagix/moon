# TODO

line-of-sight?
cdq.entity.state.player-moving -> controls/player-movement-vector

# handle-txs!

@ spawn-entity!
cdq.render.player-state-handle-click
cdq.render.remove-destroyed-entities
cdq.render.tick-entities
cdq.tx.deal-damage
cdq.tx.effect
cdq.tx.event
cdq.ui.windows.inventory (state/clicked-inventory-cell )

# effect/useful?

ctx/raycaster

# effect/handle

ctx/elapsed-time ( but can add temp-modifier component in a tx itself )
creatures-in-los-of-player -> line-of-sight?

# entity/tick

ctx/elapsed-time
ctx/grid
ctx/delta-time
ctx/max-speed
FIX - cdq.entity.state.player-moving -> controls/player-movement-vector

# entity/create

only elapsed time

# render - entities -> all kind of keys ...

# :ctx/elapsed-time

info-segment -> takes only world/elapsed-time
effect/handle - checked
entity/tick! - checked
entity/create - checked
entity/render-info! -
cdq.render.update-time
cdq.tx.add-text-effect -> handle-txs?!?!?
cdq.tx.set-cooldown
cdq.tx.spawn-alert
dev-menu

# :ctx/delta-time
entity/tick!
cdq.render.update-time
cdq.tx.update-animation

# :ctx/max-delta
cdq.render.update-time

# :ctx/max-speed
entity/tick

# :ctx/minimum-size
spawn-entity!

# :ctx/paused
cdq.render.assoc-paused
cdq.render.tick-entities
cdq.render.update-potential-fields
cdq.render.update-time
dev-menu

# :ctx/tiled-map
dispose
cdq.render.draw-world-map

# :ctx/grid
context add/remove/moved
cdq.ctx.grid
entity/tick!
cdq.render.draw-on-world-viewport.draw-cell-debug
cdq.render.draw-on-world-viewport.geom-test
cdq.render.draw-on-world-viewport.highlight-mouseover-tile
cdq.render.update-mouseover-entity
cdq.render.update-potential-fields

# :ctx/raycaster
line-of-sight?
cdq.ctx.tile-color-setter
effect/useful?

# :ctx/content-grid
context add/remove/moved
cdq.render.assoc-active-entities

# :ctx/explored-tile-corners
cdq.ctx.tile-color-setter

# :ctx/id-counter
spawn-entity!

# :ctx/entity-ids
context add/remove/moved
cdq.render.remove-destroyed-entities

# :ctx/potential-field-cache
update-potential-fields!

# :ctx/factions-iterations
cdq.render.draw-on-world-viewport.draw-cell-debug
update-potential-fields!

# :ctx/z-orders
spawn-entity!

# :ctx/render-z-order
cdq.render.draw-on-world-viewport.render-entities
cdq.render.update-mouseover-entity

# :ctx/mouseover-eid
player-effect-ctx
interaction-state
cdq.render.update-mouseover-entity
dev-menu
cdq.ui.windows.entity-info

# :ctx/player-eid
cdq.ctx.clickable-entity
creatures-in-los-of-player
cdq.entity.mouseover
cdq.render.assoc-active-entities
cdq.render.assoc-paused
cdq.render.draw-on-world-viewport.render-entities
cdq.render.player-state-handle-click
cdq.render.set-camera-on-player
cdq.render.update-mouseover-entity
cdq.ui.hp-mana-bar
cdq.ui.player-state-draw
cdq.ui.windows.inventory

# :ctx/active-entities
creatures-in-los-of-player
cdq.render.assoc-active-entities
cdq.render.draw-on-world-viewport.render-entities
cdq.render.tick-entities
cdq.render.update-potential-fields
