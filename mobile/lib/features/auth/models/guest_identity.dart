class GuestIdentity {
  String guestName;
  String? frontImageUrl;
  String? backImageUrl;
  bool isVNeID; // Flag để biết là chụp CCCD hay quét VNeID giả lập

  GuestIdentity({required this.guestName, this.frontImageUrl, this.backImageUrl, this.isVNeID = false});
}