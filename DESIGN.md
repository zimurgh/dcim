= DCIM

== Goals

Re-design
Staging changes is not a hassle
    * Saving independent of validation
    * 
Validation issues are traceable and diagnosable

Bulk updates

Untracked - Not apart of a change spec action
Modified - Is a patch, but not in a change spec yet. 
Staging - A change spec with patches attached, that is not commited yet
Pending Billing
Commitlog - 


== Project Layout

client/
server/


== Motivation

=== Change Spec inflexibilty

1) Migrations don't really work - A switch migration was originally intended to allow for updating aspects of a cross connect that has billing impact or the cross connect name (latency, speed, patch panel, mdf patch number). In practice, switch migrations involve changing way more than just a couple fields relevant to billing or unique naming. For example, the switch changes and so do many other details - it's practically a new cross connect. 

2) Can't ammend change specs - Often corrections/mistakes make it through the stages of a change spec lifetime during which it can be modified. Once the mistake or correction is realized, it's too late and the change spec is already commited.

3) Firm Transfers don't really work - When a firm gets updated, the change doesn't propagate in any way to cross connects and services that were owned/billed by/for that firm. Either firms need a more stable id or id changes need to be propagated automatically as part of the process of changing the firm.

=== Inability to efficiently work with data

1) No way to perform bulk updates properly - If 10 cross connects need their extranet switches changed, they're currently required to go into each record and update the switch info.

2) Copy/Paste doesn't work in a majority of places in UI

== Design

**Q: Should it be possible to modify history without change specs?**
A: No, so that there's only one editor in the app. Simpler, consistent mental model.
To control complexity, common editing & validation code can be shared.
For asset-specific code, editing/validation logic must be kept separate from workflow logic
Asset-specific editing/validation logic should live in domain-specific modules.
Workflow logic should be data-driven by a workflow lookup table that's part of the dcim's schema.

**Q: When an asset record is sent to client, what should be sent?**
Options:
* Send the db entity as is, references and all.
    * Simple, but also slow
    * Simple because the entities bring a chunk of the schema to the code, ad-hoc queries can be done in the field.
    * Complex because everything ends up aware of the schema. 
* Send a custom DTO that is a shallow representation of the stored entity.
    * Have to write a separate DTO / view for each entity
    * On the other hand, a DTO / view may not be needed for every entity
    * Extra layer if indirection allows for isolating changes at a api boundary
    * Will 
* Send a compressed custom DTO, with just IDs into lookup tables that are also send.
    * Easy to do if schema is organized consistently around actual ids as primary keys.
    *  
A: Send a custom DTO via a view. Keep DTO shallow and encode both names and ids for things.

**Q: When an asset record is sent to the server, what should be sent?**
A: An asset id, a asset history id, a json diff of changes.  

Every asset is stored in the database as an append-only ledger.



Change Spec editing process goes as follows:

User can create one or more a change spec item 
change spec items exist as json blobs annotated by their asset type and the asset id they're targeting. They can be optionally overlayed over assets their targeting.
User can freely move change spec items around between change specs.


add/update/terminate



Change Spec

Change Spec Item

Data Center

Cage

Rack

Device
Device Type

Port
Port Type

Cross Connect
Cross Connect Type

Market Data Feed
Market Data Feed Type


== Tech Stack

=== Client
Angular 21
ag-grid 36 Enterprise
Spartan NG
Tailwind CSS v4

=== Server
Database: Mariadb
Spring Boot 4
Java 25
Liquibase

